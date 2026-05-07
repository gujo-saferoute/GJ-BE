package com.example.safe_route_project.data.disaster.model

data class DisasterAlert(
    val fingerprint: String,
    val title: String,
    val message: String,
    val source: String,
    val region: String,
    val createdAt: String,
    val regYmd: String,
    val sn: Long?
)