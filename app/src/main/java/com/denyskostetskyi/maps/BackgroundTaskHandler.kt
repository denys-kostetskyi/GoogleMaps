package com.denyskostetskyi.maps

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.denyskostetskyi.maps.utils.JsonUtils
import com.google.android.gms.maps.model.LatLng

class BackgroundTaskHandler(
    looper: Looper,
    private val contentResolver: ContentResolver
) : Handler(looper) {
    fun postSaveMarkersToFile(uri: Uri, markerPositions: List<LatLng>) {
        post {
            val json = JsonUtils.toJson(markerPositions)
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
        }
    }

    fun postLoadMarkersFromFile(uri: Uri, callback: (List<LatLng>) -> Unit) {
        post {
            val json = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            val markerPositions = json?.let {
                JsonUtils.fromJson(it, Array<LatLng>::class.java).toList()
            } ?: emptyList()
            callback(markerPositions)
        }
    }
}
