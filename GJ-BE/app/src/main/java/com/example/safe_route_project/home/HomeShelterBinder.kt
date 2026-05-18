package com.example.safe_route_project.home

import android.location.Location
import android.widget.TextView
import com.example.safe_route_project.data.shelter.ShelterPin
import com.skt.tmap.TMapPoint

class HomeShelterBinder(
    private val shelterOneName: TextView,
    private val shelterOneDetail: TextView,
    private val shelterOneDistance: TextView,
    private val shelterTwoName: TextView,
    private val shelterTwoDetail: TextView,
    private val shelterTwoDistance: TextView,
) {

    fun render(
        shelters: List<ShelterPin>,
        basePoint: TMapPoint,
        routeDistances: Map<String, Double>
    ) {
        val nearestShelters = shelters
            .map { shelter ->
                val distance = routeDistances[shelter.markerId]
                    ?: distanceBetween(basePoint, shelter.point).toDouble()
                shelter to distance
            }
            .sortedBy { (_, distance) -> distance }

        bindRow(
            nearestShelters.getOrNull(0),
            shelterOneName,
            shelterOneDetail,
            shelterOneDistance
        )

        bindRow(
            nearestShelters.getOrNull(1),
            shelterTwoName,
            shelterTwoDetail,
            shelterTwoDistance
        )
    }

    private fun bindRow(
        shelterDistance: Pair<ShelterPin, Double>?,
        nameView: TextView,
        detailView: TextView,
        distanceView: TextView
    ) {
        if (shelterDistance == null) {
            nameView.text = "-"
            detailView.text = "표시할 대피소가 없습니다"
            distanceView.text = "-"
            return
        }

        val (shelter, distanceMeters) = shelterDistance
        nameView.text = shelter.name
        detailView.text = "${shelter.disasterLabels()} · ${shelter.address}"
        distanceView.text = formatDistance(distanceMeters)
    }

    private fun distanceBetween(startPoint: TMapPoint, endPoint: TMapPoint): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            startPoint.latitude,
            startPoint.longitude,
            endPoint.latitude,
            endPoint.longitude,
            results
        )
        return results[0]
    }

    private fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters >= 1000.0) {
            String.format("%.1fkm", distanceMeters / 1000.0)
        } else {
            "${distanceMeters.toInt()}m"
        }
    }
}