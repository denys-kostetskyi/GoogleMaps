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
) {
    private val marker: Marker
    private val circle: Circle

    init {
        marker = addMarker(position, map)
        circle = addCircle(position, radius, map)
        addToMap()
        if (!isOnMarkerDragListenerSet) {
            setOnMarkerDragListener(map)
        }
    }

    private fun addMarker(position: LatLng, map: GoogleMap) = map.addMarker(
        MarkerOptions()
            .position(position)
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
            override fun onMarkerDragStart(marker: Marker) {
            }

            override fun onMarkerDrag(marker: Marker) {
                updateCirclePosition(marker)
            }

            override fun onMarkerDragEnd(marker: Marker) {
                updateCirclePosition(marker)
            }
        })
        isOnMarkerDragListenerSet = true
    }

    private fun updateCirclePosition(marker: Marker) {
        val circle = markerToCircleMap[marker]
        circle?.center = marker.position
    }

    private fun remove() {
        markerToCircleMap.remove(marker)
        marker.remove()
        circle.remove()
    }

    companion object {
        private const val MARKER_CIRCLE_BORDER_COLOR = Color.RED
        private val MARKER_CIRCLE_FILL_COLOR = Color.argb(64, 255, 0, 0)
        private const val MARKER_CIRCLE_RADIUS_METERS = 100.0

        private val markerToCircleMap = mutableMapOf<Marker, Circle>()
        private var isOnMarkerDragListenerSet = false

        fun GoogleMap.addMarkerWithRadius(
            position: LatLng,
            radius: Double = MARKER_CIRCLE_RADIUS_METERS
        ) {
            MarkerWithRadius(position, radius, this)
        }
    }
}
