package com.denyskostetskyi.maps.presentation.utils

import com.google.gson.Gson

object JsonUtils {
    private val gson = Gson()

    fun <T> toJson(data: T): String {
        return gson.toJson(data)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }
}
