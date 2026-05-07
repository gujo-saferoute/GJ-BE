package com.example.safe_route_project.data.disaster.model

import com.google.gson.annotations.SerializedName

data class DisasterMessageResponse(
    @SerializedName("header") val header: DisasterResponseHeader? = null,
    @SerializedName("numOfRows") val numOfRows: Int? = null,
    @SerializedName("pageNo") val pageNo: Int? = null,
    @SerializedName("totalCount") val totalCount: Int? = null,
    @SerializedName("body") val body: List<DisasterMessageDto>? = null,
)

data class DisasterResponseHeader(
    @SerializedName("resultMsg") val resultMsg: String? = null,
    @SerializedName("resultCode") val resultCode: String? = null,
    @SerializedName("errorMsg") val errorMsg: String? = null,
)