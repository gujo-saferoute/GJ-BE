package com.example.safe_route_project.data.disaster.repository

import android.content.Context
import android.util.Log
import com.example.safe_route_project.BuildConfig
import com.example.safe_route_project.data.disaster.api.DisasterApiContract
import com.example.safe_route_project.data.disaster.api.DisasterMessageApi
import com.example.safe_route_project.data.disaster.model.DisasterAlert
import com.example.safe_route_project.data.disaster.model.DisasterMessageDto
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DisasterMessageRepository(
    private val api: DisasterMessageApi,
) {

    suspend fun getLatestAlert(): DisasterAlert? {
        if (BuildConfig.DISASTER_API_PATH.isBlank() || BuildConfig.DISASTER_API_KEY.isBlank()) {
            Log.e("DisasterApi", "API path 또는 API key가 비어 있음")
            return null
        }

        return fetchLatestFilteredAlert(
            url = buildApiUrl(),
            query = buildQueryParams(),
            headers = buildHeaders(),
            logPrefix = "DisasterApi"
        )
    }

    suspend fun getLatestTestAlert(): DisasterAlert? {
        return fetchLatestFilteredAlert(
            url = TEST_MODE_URL,
            query = mapOf(
                DisasterApiContract.RETURN_TYPE_NAME to DisasterApiContract.RETURN_TYPE_JSON
            ),
            headers = emptyMap(),
            logPrefix = "DisasterApiTest"
        )
    }

    private suspend fun fetchLatestFilteredAlert(
        url: String,
        query: Map<String, String>,
        headers: Map<String, String>,
        logPrefix: String
    ): DisasterAlert? {
        Log.d(logPrefix, "url=$url")
        Log.d(logPrefix, "query=$query")
        Log.d(logPrefix, "headers=$headers")

        val response = api.getDisasterMessages(
            url = url,
            query = query,
            headers = headers
        )

        Log.d(
            logPrefix,
            "resultCode=${response.header?.resultCode}, resultMsg=${response.header?.resultMsg}, totalCount=${response.totalCount}, bodyCount=${response.body?.size ?: -1}"
        )

        val items = response.body.orEmpty()
            .filter { isGangwonRegion(it.rcptnRgnNm) }
            .filter { isDangerousDisaster(it.dstSeNm) }

        if (items.isEmpty()) return null

        val latestItem = items.maxWithOrNull(
            compareBy<DisasterMessageDto> {
                parseToEpochMillis(it.crtDt ?: it.regYmd ?: it.mdfcnYmd)
            }.thenBy { it.sn ?: 0L }
        ) ?: return null

        Log.d(
            logPrefix,
            "latest sn=${latestItem.sn}, crtDt=${latestItem.crtDt}, regYmd=${latestItem.regYmd}, dst=${latestItem.dstSeNm}"
        )

        return latestItem.toDomain()
    }

    private fun buildApiUrl(): String {
        val path = BuildConfig.DISASTER_API_PATH
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        return BuildConfig.DISASTER_API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
    }

    private fun buildQueryParams(): Map<String, String> {
        val params: LinkedHashMap<String, String> = linkedMapOf(
            DisasterApiContract.PAGE_NO_NAME to DisasterApiContract.PAGE_NO,
            DisasterApiContract.NUM_OF_ROWS_NAME to DisasterApiContract.NUM_OF_ROWS,
            DisasterApiContract.RETURN_TYPE_NAME to DisasterApiContract.RETURN_TYPE_JSON,
            DisasterApiContract.QUERY_DATE_NAME to currentQueryDate(),
        )

        if (DisasterApiContract.AUTH_MODE == DisasterApiContract.AUTH_MODE_QUERY) {
            params[DisasterApiContract.AUTH_QUERY_NAME] = BuildConfig.DISASTER_API_KEY
        }

        return params
    }

    private fun buildHeaders(): Map<String, String> {
        if (DisasterApiContract.AUTH_MODE != DisasterApiContract.AUTH_MODE_HEADER) {
            return emptyMap()
        }
        return mapOf(DisasterApiContract.AUTH_HEADER_NAME to BuildConfig.DISASTER_API_KEY)
    }

    private fun currentQueryDate(): String {
        val seoulZone = ZoneId.of("Asia/Seoul")
        return LocalDate.now(seoulZone)
            .minusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

    private fun DisasterMessageDto.toDomain(): DisasterAlert {
        val title = emrgStepNm?.takeIf { it.isNotBlank() }
            ?: dstSeNm?.takeIf { it.isNotBlank() }
            ?: "재난문자"

        val message = msgCn?.trim().orEmpty()

        val region = rcptnRgnNm
            ?.trim()
            ?.removeSuffix(",")
            ?.takeIf { it.isNotBlank() }
            ?: "수신 지역 정보 없음"

        val displayBaseDate = crtDt ?: regYmd ?: mdfcnYmd
        val createdAtText = normalizeDisplayDate(displayBaseDate)
        val source = "$createdAtText · $region"

        val fingerprint = listOf(
            sn?.toString().orEmpty(),
            crtDt.orEmpty(),
            regYmd.orEmpty(),
            message
        ).joinToString("|")

        return DisasterAlert(
            fingerprint = fingerprint,
            title = title,
            message = message,
            source = source,
            region = region,
            createdAt = crtDt.orEmpty(),
            regYmd = regYmd.orEmpty(),
            sn = sn,
        )
    }

    companion object {

        private const val TEST_MODE_URL =
            "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247?serviceKey=AGDIVZLYK121C979&pageNo=1&numOfRows=45&rgnNm=%EA%B0%95%EC%9B%90"

        fun isGangwonRegion(region: String?): Boolean {
            if (region.isNullOrBlank()) return false
            return region.contains("강원")
        }

        private val DANGEROUS_DISASTER_TYPES = setOf(
            "지진", "지진해일", "홍수", "호우", "태풍", "산사태",
            "화재", "폭발", "붕괴", "테러", "민방공", "풍수해",
            "대설", "강풍", "해일", "화산", "산불"
        )

        fun isDangerousDisaster(dstSeNm: String?): Boolean {
            if (dstSeNm.isNullOrBlank()) return false
            return DANGEROUS_DISASTER_TYPES.any { dstSeNm.contains(it) }
        }

        fun create(context: Context): DisasterMessageRepository {
            val client = OkHttpClient.Builder().build()

            val retrofit = Retrofit.Builder()
                .baseUrl(
                    BuildConfig.DISASTER_API_BASE_URL.ifBlank {
                        "https://www.safetydata.go.kr/"
                    }
                )
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return DisasterMessageRepository(
                api = retrofit.create(DisasterMessageApi::class.java)
            )
        }

        fun parseToEpochMillis(value: String?): Long {
            val dateTime = parseToLocalDateTime(value) ?: return Long.MIN_VALUE
            return dateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
        }

        private fun parseToLocalDateTime(value: String?): LocalDateTime? {
            if (value.isNullOrBlank()) return null

            val trimmed = value.trim()

            return try {
                when {
                    trimmed.contains("/") && trimmed.contains(":") -> {
                        val normalized = trimmed.substring(0, minOf(trimmed.length, 19))
                        LocalDateTime.parse(
                            normalized,
                            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                        )
                    }

                    trimmed.contains("-") && trimmed.contains(":") -> {
                        val normalized = trimmed.substring(0, minOf(trimmed.length, 19))
                        LocalDateTime.parse(
                            normalized,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        )
                    }

                    trimmed.contains("/") -> {
                        LocalDate.parse(
                            trimmed,
                            DateTimeFormatter.ofPattern("yyyy/MM/dd")
                        ).atStartOfDay()
                    }

                    trimmed.contains("-") -> {
                        val normalized = trimmed.substring(0, minOf(trimmed.length, 10))
                        LocalDate.parse(
                            normalized,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        ).atStartOfDay()
                    }

                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }

        fun normalizeDisplayDate(value: String?): String {
            val dateTime = parseToLocalDateTime(value) ?: return "발송시각 미상"
            return dateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
        }
    }
}