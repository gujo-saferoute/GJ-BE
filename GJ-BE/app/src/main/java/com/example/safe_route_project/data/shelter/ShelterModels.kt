package com.example.safe_route_project.data.shelter

import com.skt.tmap.TMapPoint

data class ShelterPin(
    val markerId: String,
    val name: String,
    val address: String,
    val description: String,
    val point: TMapPoint,
    val barrierFree: Boolean,
    val evalInfo: String = ""
)

data class RouteResult(
    val points: ArrayList<TMapPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double
)
