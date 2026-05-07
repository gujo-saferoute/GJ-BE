package com.example.safe_route_project.data.disaster.model

import com.google.gson.annotations.SerializedName

data class DisasterMessageDto(
    @SerializedName("MSG_CN") val msgCn: String? = null,
    @SerializedName("RCPTN_RGN_NM") val rcptnRgnNm: String? = null,
    @SerializedName("CRT_DT") val crtDt: String? = null,
    @SerializedName("REG_YMD") val regYmd: String? = null,
    @SerializedName("EMRG_STEP_NM") val emrgStepNm: String? = null,
    @SerializedName("SN") val sn: Long? = null,
    @SerializedName("DST_SE_NM") val dstSeNm: String? = null,
    @SerializedName("MDFCN_YMD") val mdfcnYmd: String? = null,
)