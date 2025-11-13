package com.example.i_radar  // your actual package name

import java.util.Date

data class ReferencePoint(
    val name: String,
    val lat: Double,
    val lon: Double,
    val lastUpdate: Date? = null,             // <- use Date instead of Timestamp
    val requestUpdate: Boolean = false,          // default false
    val requestFrom: List<String> = emptyList()  // default empty list
)
