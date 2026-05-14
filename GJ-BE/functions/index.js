const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();

const DANGEROUS_DISASTER_TYPES = [
  "지진", "지진해일", "홍수", "호우", "태풍", "산사태",
  "화재", "폭발", "붕괴", "테러", "민방공", "풍수해",
  "대설", "강풍", "해일", "화산"
];

function isGangwonRegion(region) {
  if (!region) return false;
  return region.includes("강원");
}

function isDangerousDisaster(dstSeNm) {
  if (!dstSeNm) return false;
  return DANGEROUS_DISASTER_TYPES.some((type) => dstSeNm.includes(type));
}

exports.checkDisasterAlert = onSchedule(
    {
      schedule: "every 2 minutes",
      region: "asia-northeast3",
      timeZone: "Asia/Seoul",
    },
    async (event) => {
      const SERVICE_KEY = process.env.DISASTER_API_KEY;
      const url = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247";

      const today = new Date();
      today.setHours(today.getHours() + 9);
      const crtDt = today.toISOString().slice(0, 10).replace(/-/g, "");

      try {
        const response = await axios.get(url, {
          params: {
            serviceKey: SERVICE_KEY,
            returnType: "json",
            pageNo: "1",
            numOfRows: "60",
            crtDt: crtDt,
          },
        });

        const items = (response.data.body || [])
            .filter((item) => isGangwonRegion(item.RCPTN_RGN_NM))
            .filter((item) => isDangerousDisaster(item.DST_SE_NM));

        if (items.length === 0) {
          console.log("필터링된 재난문자 없음");
          return null;
        }

        const latest = items.reduce((a, b) => {
          const aTime = a.CRT_DT || a.REG_YMD || "";
          const bTime = b.CRT_DT || b.REG_YMD || "";
          return aTime > bTime ? a : b;
        });

        const fingerprint = `${latest.SN}|${latest.CRT_DT}|${latest.MSG_CN}`;

        const db = admin.firestore();
        const lastRef = db.collection("disaster_state").doc("last_alert");
        const lastDoc = await lastRef.get();

        if (lastDoc.exists && lastDoc.data().fingerprint === fingerprint) {
          console.log("동일한 재난문자 — 알림 생략");
          return null;
        }

        await lastRef.set({ fingerprint, updatedAt: new Date() });

        const title = latest.EMRG_STEP_NM || latest.DST_SE_NM || "재난문자";
        const message = (latest.MSG_CN || "").trim();
        const region = (latest.RCPTN_RGN_NM || "").trim();

        await admin.messaging().sendToTopic("disaster_alerts", {
          notification: {
            title: `🚨 ${title}`,
            body: message,
          },
          data: {
            type: "disaster_alert",
            region: region,
            open_tab: "map",
          },
        });

        console.log(`FCM 발송 완료: ${title} - ${region}`);
        return null;
      } catch (error) {
        console.error("재난문자 API 오류:", error.message);
        return null;
      }
    });