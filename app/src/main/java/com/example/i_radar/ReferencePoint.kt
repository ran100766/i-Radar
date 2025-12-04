package com.example.i_radar  // your actual package name

import java.util.Date

data class ReferencePoint(
    val name: String,
    val lat: Double,
    val lon: Double,
//    val isOnline: Boolean = false,
    val lastUpdate: Date? = null            // <- use Date instead of Timestamp
)
