"""
장애인편의시설 API로 대피소 배리어프리 정보 업데이트 스크립트

사용법:
1. 이 파일을 serviceAccountKey.json과 같은 폴더에 넣기
2. python update_barrier_free.py
"""

import requests
import firebase_admin
from firebase_admin import credentials, firestore
import time
import xml.etree.ElementTree as ET

# ===== 설정 =====
SERVICE_ACCOUNT_KEY = "safe-route-31943-firebase-adminsdk-fbsvc-ab0a27ad1c.json"
API_KEY = "8a2cf12c1dd2a7a9ad56cd788c16e5b0cc35dfec2380cf8a2ad8fdbef18a76d9"
LIST_URL = "http://apis.data.go.kr/B554287/DisabledPersonConvenientFacility/getDisConvFaclList"
DETAIL_URL = "http://apis.data.go.kr/B554287/DisabledPersonConvenientFacility/getFacInfoOpenApiJpEvalInfoList"
# =================

def get_wfclt_id(faclNm):
    """시설명으로 wfcltId 조회"""
    try:
        params = {
            'serviceKey': API_KEY,
            'faclNm': faclNm,
            'numOfRows': '1',
            'pageNo': '1'
        }
        res = requests.get(LIST_URL, params=params, timeout=10)
        root = ET.fromstring(res.text)

        serv = root.find('.//servList')
        if serv is None:
            return None

        wfclt_id = serv.findtext('wfcltId')
        return wfclt_id
    except Exception as e:
        print(f"  목록조회 오류: {e}")
        return None


def get_eval_info(wfclt_id):
    """wfcltId로 배리어프리 기구표 조회"""
    try:
        params = {
            'serviceKey': API_KEY,
            'wfcltId': wfclt_id,
        }
        res = requests.get(DETAIL_URL, params=params, timeout=10)
        root = ET.fromstring(res.text)

        serv = root.find('.//servList')
        if serv is None:
            return None

        eval_info = serv.findtext('evalInfo')
        return eval_info
    except Exception as e:
        print(f"  기구표 조회 오류: {e}")
        return None


def main():
    # Firebase 초기화
    cred = credentials.Certificate(SERVICE_ACCOUNT_KEY)
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    # Firestore에서 대피소 목록 읽기
    print("Firestore에서 대피소 목록 읽는 중...")
    docs = list(db.collection("shelters").stream())
    print(f"총 {len(docs)}개 대피소 처리 시작\n")

    success = 0
    barrier_free_count = 0
    no_match = 0

    for i, doc in enumerate(docs):
        data = doc.to_dict()
        name = data.get('name', '')

        # 아파트 지하주차장 같은 경우 시설명에서 핵심 이름만 추출
        # 예: "라이프타운아파트 지하주차장 1층" → "라이프타운아파트"
        search_name = name.split(' ')[0] if ' ' in name else name

        print(f"[{i+1}/{len(docs)}] {name} 검색 중...")

        # 1단계: wfcltId 조회
        wfclt_id = get_wfclt_id(search_name)
        if not wfclt_id:
            print(f"  → 매칭 없음")
            no_match += 1
            time.sleep(0.1)
            continue

        # 2단계: evalInfo 조회
        eval_info = get_eval_info(wfclt_id)

        # 3단계: Firestore 업데이트
        update_data = {
            'wfcltId': wfclt_id,
        }

        if eval_info:
            update_data['barrierFree'] = True
            update_data['evalInfo'] = eval_info
            barrier_free_count += 1
            print(f"  → 배리어프리 ✓ ({eval_info[:50]}...)")
        else:
            update_data['barrierFree'] = False
            update_data['evalInfo'] = ''
            print(f"  → 배리어프리 정보 없음")

        db.collection("shelters").document(doc.id).update(update_data)
        success += 1

        # API 과호출 방지
        time.sleep(0.2)

    print(f"\n=== 완료 ===")
    print(f"처리 성공: {success}개")
    print(f"배리어프리 확인: {barrier_free_count}개")
    print(f"매칭 없음: {no_match}개")


if __name__ == "__main__":
    main()
