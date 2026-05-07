package com.example.safe_route_project.data.disaster.api

object DisasterApiContract {
    const val AUTH_MODE_QUERY = "query"
    const val AUTH_MODE_HEADER = "header"

    const val AUTH_MODE = AUTH_MODE_QUERY
    const val AUTH_QUERY_NAME = "serviceKey"
    const val AUTH_HEADER_NAME = "Authorization"

    const val PAGE_NO_NAME = "pageNo"
    const val NUM_OF_ROWS_NAME = "numOfRows"
    const val QUERY_DATE_NAME = "crtDt"
    const val RETURN_TYPE_NAME = "returnType"
    const val REGION_NAME = "rgnNm"

    const val PAGE_NO = "1"
    const val NUM_OF_ROWS = "60"
    const val RETURN_TYPE_JSON = "json"
}