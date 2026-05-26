package com.example.safe_route_project.home

import android.location.Location
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.safe_route_project.R
import com.example.safe_route_project.data.shelter.ShelterPin
import com.skt.tmap.TMapPoint

class HomeShelterBinder(
    private val shelterOneName: TextView,
    private val shelterOneDetail: TextView,
    private val shelterOneAction: TextView,
    private val shelterTwoName: TextView,
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
            nearestShelters.getOrNull(0),
            shelterOneName,
            shelterOneDetail,
            shelterOneAction,
            onRouteShortcutClick
        )

        bindRow(
            nearestShelters.getOrNull(1),
            shelterTwoName,
            shelterTwoDetail,
            shelterTwoAction,
            onRouteShortcutClick
        )
    }

    private fun bindRow(
        shelterDistance: Pair<ShelterPin, Double>?,
        nameView: TextView,
        detailView: TextView,
        actionView: TextView,
        onRouteShortcutClick: (ShelterPin) -> Unit
    ) {
        if (shelterDistance == null) {
            nameView.text = "-"
            detailView.text = "표시할 대피소가 없습니다"
            actionView.text = actionView.context.getString(R.string.route_shortcut)
            actionView.isEnabled = false
            actionView.alpha = 0.4f
            actionView.setOnClickListener(null)
            return
        }

        val (shelter, distanceMeters) = shelterDistance
        nameView.text = shelter.name
        detailView.text = buildFacilitySummary(detailView, shelter, formatDistance(distanceMeters))
        actionView.text = actionView.context.getString(R.string.route_shortcut)
        actionView.isEnabled = true
        actionView.alpha = 1f
        actionView.setOnClickListener { onRouteShortcutClick(shelter) }
    }

    private fun buildFacilitySummary(
        view: TextView,
        shelter: ShelterPin,
        distanceText: String
    ): CharSequence {
        val builder = SpannableStringBuilder()
        val tags = shelter.evalInfo
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (shelter.barrierFree && tags.isNotEmpty()) {
            var hasPrevious = false

            if (tags.contains("승강기")) {
                appendFacilityWithIcon(builder, view, R.drawable.ic_barrier_elevator, "승강기", hasPrevious)
                hasPrevious = true
            }

            if (tags.contains("장애인전용주차구역")) {
                appendFacilityWithIcon(builder, view, R.drawable.ic_barrier_parking, "장애인전용주차구역", hasPrevious)
                hasPrevious = true
            }

            if (tags.contains("장애인사용가능화장실")) {
                appendFacilityWithIcon(builder, view, R.drawable.ic_barrier_toilet, "장애인사용가능화장실", hasPrevious)
                hasPrevious = true
            }

            val entranceTags = listOf(
                "주출입구 높이차이 제거",
                "주출입구 접근로",
                "주출입구(문)"
            ).filter { tags.contains(it) }

            if (entranceTags.isNotEmpty()) {
                appendFacilityWithIcon(builder, view, R.drawable.ic_barrier_entrance, "주출입구", hasPrevious)
            }
        }

        if (builder.isEmpty()) {
            builder.append(view.context.getString(R.string.barrier_free_facility_none))
        }

        builder.append(" · ")
        builder.append(distanceText)
        return builder
    }

    private fun appendFacilityWithIcon(
        builder: SpannableStringBuilder,
        view: TextView,
        drawableRes: Int,
        text: String,
        addComma: Boolean
    ) {
        if (addComma) {
            builder.append(", ")
        }

        val drawable = ContextCompat.getDrawable(view.context, drawableRes) ?: return
        val size = (20 * view.resources.displayMetrics.density).toInt()
        drawable.setBounds(0, 0, size, size)

        val start = builder.length
        builder.append("\uFFFC")
        val end = builder.length
        builder.setSpan(
            ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.append("\u00A0")
        builder.append(text)
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
