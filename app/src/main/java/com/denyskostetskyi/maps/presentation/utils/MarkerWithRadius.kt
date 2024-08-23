package com.denyskostetskyi.maps.presentation.utils

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MarkerWithRadius private constructor(
    position: LatLng,
    radius: Double,
    map: GoogleMap,
    private val shouldDeleteMarker: () -> Boolean,
) {
    private val marker: Marker
    private val circle: Circle

    init {
        marker = addMarker(position, map)
        circle = addCircle(position, radius, map)
        addToMap()
        if (!isOnMarkerDragListenerSet) {
            setOnMarkerDragListener(map)
            setOnMarkerClickListener(map)
        }
    }

    private fun addMarker(position: LatLng, map: GoogleMap) = map.addMarker(
        MarkerOptions()
            .position(position)
            .snippet("Delete")
            .draggable(true)
    ) ?: throw IllegalStateException("Marker could not be created")

    private fun addCircle(position: LatLng, radius: Double, map: GoogleMap) = map.addCircle(
        CircleOptions()
            .center(position)
            .radius(radius)
            .strokeColor(MARKER_CIRCLE_BORDER_COLOR)
            .fillColor(MARKER_CIRCLE_FILL_COLOR)
    )

    private fun addToMap() {
        markerToCircleMap[marker] = circle
    }

    private fun setOnMarkerDragListener(map: GoogleMap) {
        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            private var circle: Circle? = null

            private fun updateCirclePosition(position: LatLng) {
                circle?.center = position
            }

            override fun onMarkerDragStart(marker: Marker) {
                circle = markerToCircleMap[marker]
            }

            override fun onMarkerDrag(marker: Marker) {
                updateCirclePosition(marker.position)
            }

            override fun onMarkerDragEnd(marker: Marker) {
                updateCirclePosition(marker.position)
                circle = null
            }
        })
        isOnMarkerDragListenerSet = true
    }

    private fun setOnMarkerClickListener(map: GoogleMap) {
        map.setOnMarkerClickListener { currentMarker ->
            if (shouldDeleteMarker()) {
                val circle = markerToCircleMap.remove(currentMarker)
                currentMarker.remove()
                circle?.remove()
                true
            } else {
                false
            }
        }
    }

    companion object {
        private const val MARKER_CIRCLE_BORDER_COLOR = Color.RED
        private val MARKER_CIRCLE_FILL_COLOR = Color.argb(64, 255, 0, 0)
        private const val MARKER_CIRCLE_RADIUS_METERS = 100.0

        private val markerToCircleMap = mutableMapOf<Marker, Circle>()
        private var isOnMarkerDragListenerSet = false

        fun GoogleMap.addMarkerWithRadius(
            position: LatLng,
            radius: Double = MARKER_CIRCLE_RADIUS_METERS,
            shouldDeleteMarker: () -> Boolean,
        ) {
            MarkerWithRadius(position, radius, this, shouldDeleteMarker)
        }
    }
}
