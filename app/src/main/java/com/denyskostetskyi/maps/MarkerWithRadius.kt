package com.denyskostetskyi.maps

import android.graphics.Color
import com.denyskostetskyi.maps.model.MarkerData
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
        putIntoMap()
        if (!areMarkerTouchListenersSet) {
            setOnMarkerDragListener(map)
            setOnMarkerClickListener(map)
            areMarkerTouchListenersSet = true
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

    private fun putIntoMap() {
        markerToCircleMap[marker] = circle
    }

    private fun setOnMarkerDragListener(map: GoogleMap) {
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

    private fun setOnMarkerClickListener(map: GoogleMap) {
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

    companion object {
        private const val MARKER_CIRCLE_RADIUS_METERS = 100.0
        private const val MARKER_CIRCLE_BORDER_COLOR = Color.RED
        private val MARKER_CIRCLE_FILL_COLOR = Color.argb(64, 255, 0, 0)

        private val markerToCircleMap = mutableMapOf<Marker, Circle>()
        private var areMarkerTouchListenersSet = false

        val markersData
            get() = markerToCircleMap.keys.map {
                MarkerData(
                    it.position.latitude,
                    it.position.longitude
                )
            }

        fun reset() {
            markerToCircleMap.clear()
            areMarkerTouchListenersSet = false
        }

        fun GoogleMap.addMarkerWithRadius(
            position: LatLng,
            radius: Double = MARKER_CIRCLE_RADIUS_METERS,
            shouldDeleteMarker: () -> Boolean,
        ) {
            MarkerWithRadius(position, radius, this, shouldDeleteMarker)
        }
    }
}
