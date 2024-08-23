package com.denyskostetskyi.maps

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MarkerManager(private val map: GoogleMap, private val shouldDeleteMarker: () -> Boolean) {
    private val markerToCircleMap = mutableMapOf<Marker, Circle>()

    val markerPositions get() = markerToCircleMap.keys.map { it.position }

    init {
        setOnMarkerDragListener()
        setOnMarkerClickListener()
        setOnMapLongClickListener()
    }

    fun addMarkers(positions: List<LatLng>) {
        positions.forEach(::addMarker)
    }

    private fun setOnMarkerDragListener() {
        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            private var boundCircle: Circle? = null

            private fun updateCirclePosition(position: LatLng) {
                boundCircle?.center = position
            }

            override fun onMarkerDragStart(draggedMarker: Marker) {
                boundCircle = markerToCircleMap[draggedMarker]
            }

            override fun onMarkerDrag(draggedMarker: Marker) {
                updateCirclePosition(draggedMarker.position)
            }

            override fun onMarkerDragEnd(draggedMarker: Marker) {
                updateCirclePosition(draggedMarker.position)
                boundCircle = null
            }
        })
    }

    private fun setOnMarkerClickListener() {
        map.setOnMarkerClickListener { clickedMarker ->
            if (shouldDeleteMarker()) {
                val boundCircle = markerToCircleMap.remove(clickedMarker)
                clickedMarker.remove()
                boundCircle?.remove()
                true
            } else {
                false
            }
        }
    }

    private fun setOnMapLongClickListener() {
        map.setOnMapLongClickListener(::addMarker)
    }

    private fun addMarker(position: LatLng) {
        val marker = map.addMarker(
            MarkerOptions()
                .position(position)
                .draggable(true)
        ) ?: return
        val circle = map.addCircle(
            CircleOptions()
                .center(position)
                .radius(MARKER_CIRCLE_RADIUS_METERS)
                .strokeColor(MARKER_CIRCLE_BORDER_COLOR)
                .fillColor(MARKER_CIRCLE_FILL_COLOR)
        )
        putIntoMap(marker, circle)
    }

    private fun putIntoMap(marker: Marker, circle: Circle) {
        markerToCircleMap[marker] = circle
    }

    companion object {
        private const val MARKER_CIRCLE_RADIUS_METERS = 100.0
        private const val MARKER_CIRCLE_BORDER_COLOR = Color.RED
        private val MARKER_CIRCLE_FILL_COLOR = Color.argb(64, 255, 0, 0)
    }
}