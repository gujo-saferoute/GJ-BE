package com.example.safe_route_project.data.shelter

import com.skt.tmap.TMapPoint

data class ShelterPin(
    val markerId: String,
    val name: String,
    val address: String,
    val description: String,
    val point: TMapPoint,
    val disasterTypes: Set<DisasterType>,
    val barrierFree: Boolean,
    val evalInfo: String = ""
) {
    fun matchesDisasterFilter(disasterType: DisasterType?): Boolean {
        return disasterType == null || disasterTypes.contains(disasterType)
    }

    fun disasterLabels(): String {
        return disasterTypes.joinToString("/") { it.label }
    }
}

enum class DisasterType(val label: String) {
    EARTHQUAKE("지진"),
    CIVIL_DEFENSE("민방위"),
    LANDSLIDE("산사태")
}

data class RouteResult(
    val points: ArrayList<TMapPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double
)