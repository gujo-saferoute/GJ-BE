package com.example.safe_route_project.home

import android.location.Location
import android.view.View
import android.widget.ImageView
import android.widget.HorizontalScrollView
import android.widget.TextView
import com.example.safe_route_project.R
import com.example.safe_route_project.data.shelter.ShelterPin
import com.skt.tmap.TMapPoint

class HomeShelterBinder(
    private val shelterOneName: TextView,
    private val shelterOneBarrierScroll: HorizontalScrollView,
    private val shelterOneBarrierFreeIcon: ImageView,
    private val shelterOneDividerIcon: ImageView,
    private val shelterOneElevatorIcon: ImageView,
    private val shelterOneParkingIcon: ImageView,
    private val shelterOneToiletIcon: ImageView,
    private val shelterOneEntranceIcon: ImageView,
    private val shelterOneDetail: TextView,
    private val shelterOneAction: TextView,

    private val shelterTwoName: TextView,
    private val shelterTwoBarrierScroll: HorizontalScrollView,
    private val shelterTwoBarrierFreeIcon: ImageView,
    private val shelterTwoDividerIcon: ImageView,
    private val shelterTwoElevatorIcon: ImageView,
    private val shelterTwoParkingIcon: ImageView,
    private val shelterTwoToiletIcon: ImageView,
    private val shelterTwoEntranceIcon: ImageView,
    private val shelterTwoDetail: TextView,
    private val shelterTwoAction: TextView,
) {

    fun render(
        shelters: List<ShelterPin>,
        basePoint: TMapPoint,
        routeDistances: Map<String, Double>,
        onRouteShortcutClick: (ShelterPin) -> Unit
    ) {
        val nearestShelters = shelters
            .map { shelter ->
                val distance = routeDistances[shelter.markerId]
                    ?: distanceBetween(basePoint, shelter.point).toDouble()
                shelter to distance
            }
            .sortedBy { (_, distance) -> distance }

        bindRow(
            shelterDistance = nearestShelters.getOrNull(0),
            nameView = shelterOneName,
            barrierScroll = shelterOneBarrierScroll,
            barrierFreeIcon = shelterOneBarrierFreeIcon,
            dividerIcon = shelterOneDividerIcon,
            elevatorIcon = shelterOneElevatorIcon,
            parkingIcon = shelterOneParkingIcon,
            toiletIcon = shelterOneToiletIcon,
            entranceIcon = shelterOneEntranceIcon,
            detailView = shelterOneDetail,
            actionView = shelterOneAction,
            onRouteShortcutClick = onRouteShortcutClick
        )

        bindRow(
            shelterDistance = nearestShelters.getOrNull(1),
            nameView = shelterTwoName,
            barrierScroll = shelterTwoBarrierScroll,
            barrierFreeIcon = shelterTwoBarrierFreeIcon,
            dividerIcon = shelterTwoDividerIcon,
            elevatorIcon = shelterTwoElevatorIcon,
            parkingIcon = shelterTwoParkingIcon,
            toiletIcon = shelterTwoToiletIcon,
            entranceIcon = shelterTwoEntranceIcon,
            detailView = shelterTwoDetail,
            actionView = shelterTwoAction,
            onRouteShortcutClick = onRouteShortcutClick
        )
    }

    private fun bindRow(
        shelterDistance: Pair<ShelterPin, Double>?,
        nameView: TextView,
        barrierScroll: HorizontalScrollView,
        barrierFreeIcon: ImageView,
        dividerIcon: ImageView,
        elevatorIcon: ImageView,
        parkingIcon: ImageView,
        toiletIcon: ImageView,
        entranceIcon: ImageView,
        detailView: TextView,
        actionView: TextView,
        onRouteShortcutClick: (ShelterPin) -> Unit
    ) {
        if (shelterDistance == null) {
            nameView.text = "-"
            detailView.text = "표시할 대피소가 없습니다"
            hideBarrierIcons(
                barrierScroll,
                barrierFreeIcon,
                dividerIcon,
                elevatorIcon,
                parkingIcon,
                toiletIcon,
                entranceIcon
            )
            actionView.text = actionView.context.getString(R.string.route_shortcut)
            actionView.isEnabled = false
            actionView.alpha = 0.4f
            actionView.setOnClickListener(null)
            return
        }

        val (shelter, distanceMeters) = shelterDistance
        nameView.text = shelter.name
        detailView.visibility = View.VISIBLE
        detailView.text = barrierFacilityText(shelter)
        bindBarrierIcons(
            shelter,
            barrierScroll,
            barrierFreeIcon,
            dividerIcon,
            elevatorIcon,
            parkingIcon,
            toiletIcon,
            entranceIcon
        )

        actionView.text = formatDistance(distanceMeters)
        actionView.isEnabled = true
        actionView.alpha = 1f
        actionView.setOnClickListener { onRouteShortcutClick(shelter) }
    }


    private fun barrierFacilityText(shelter: ShelterPin): String {
        if (!shelter.barrierFree) {
            return shelterOneDetail.context.getString(R.string.barrier_free_facility_none)
        }

        val tags = shelter.evalInfo
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val labels = mutableListOf<String>()
        if (tags.contains("승강기")) labels.add("승강기")
        if (tags.contains("장애인전용주차구역")) labels.add("장애인전용주차구역")
        if (tags.contains("장애인사용가능화장실")) labels.add("장애인사용가능화장실")
        labels.addAll(
            listOf(
                "주출입구 높이차이 제거",
                "주출입구 접근로",
                "주출입구(문)"
            ).filter { tags.contains(it) }
        )

        return labels.ifEmpty {
            listOf(shelterOneDetail.context.getString(R.string.barrier_free_facility_none))
        }.joinToString(", ")
    }

    private fun bindBarrierIcons(
        shelter: ShelterPin,
        barrierScroll: HorizontalScrollView,
        barrierFreeIcon: ImageView,
        dividerIcon: ImageView,
        elevatorIcon: ImageView,
        parkingIcon: ImageView,
        toiletIcon: ImageView,
        entranceIcon: ImageView
    ) {
        val tags = shelter.evalInfo
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val hasElevator = tags.contains("승강기")
        val hasParking = tags.contains("장애인전용주차구역")
        val hasToilet = tags.contains("장애인사용가능화장실")
        val hasEntrance = listOf(
            "주출입구 높이차이 제거",
            "주출입구 접근로",
            "주출입구(문)"
        ).any { tags.contains(it) }

        val hasAny = shelter.barrierFree && (hasElevator || hasParking || hasToilet || hasEntrance)

        if (!hasAny) {
            hideBarrierIcons(
                barrierScroll,
                barrierFreeIcon,
                dividerIcon,
                elevatorIcon,
                parkingIcon,
                toiletIcon,
                entranceIcon
            )
            return
        }

        barrierScroll.visibility = View.VISIBLE
        barrierFreeIcon.visibility = View.VISIBLE
        dividerIcon.visibility = View.VISIBLE
        elevatorIcon.visibility = if (hasElevator) View.VISIBLE else View.GONE
        parkingIcon.visibility = if (hasParking) View.VISIBLE else View.GONE
        toiletIcon.visibility = if (hasToilet) View.VISIBLE else View.GONE
        entranceIcon.visibility = if (hasEntrance) View.VISIBLE else View.GONE
    }

    private fun hideBarrierIcons(
        barrierScroll: HorizontalScrollView,
        barrierFreeIcon: ImageView,
        dividerIcon: ImageView,
        elevatorIcon: ImageView,
        parkingIcon: ImageView,
        toiletIcon: ImageView,
        entranceIcon: ImageView
    ) {
        barrierScroll.visibility = View.GONE
        barrierFreeIcon.visibility = View.GONE
        dividerIcon.visibility = View.GONE
        elevatorIcon.visibility = View.GONE
        parkingIcon.visibility = View.GONE
        toiletIcon.visibility = View.GONE
        entranceIcon.visibility = View.GONE
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