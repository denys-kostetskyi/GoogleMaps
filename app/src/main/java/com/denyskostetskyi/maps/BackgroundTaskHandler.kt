package com.denyskostetskyi.maps

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.denyskostetskyi.maps.model.MarkerData
import com.denyskostetskyi.maps.presentation.utils.JsonUtils
import com.denyskostetskyi.maps.presentation.utils.MarkerWithRadius

class BackgroundTaskHandler(
    looper: Looper,
    private val contentResolver: ContentResolver
) : Handler(looper) {
    fun postSaveMarkersToFile(uri: Uri) {
        post {
            val data = MarkerWithRadius.markersData
            val json = JsonUtils.toJson(data)
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
        }
    }

    fun postLoadMarkersFromFile(uri: Uri, callback: (List<MarkerData>) -> Unit) {
        post {
            val json = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            val markerDataList = json?.let {
                JsonUtils.fromJson(it, Array<MarkerData>::class.java).toList()
            } ?: emptyList()
            callback(markerDataList)
        }
    }
}
