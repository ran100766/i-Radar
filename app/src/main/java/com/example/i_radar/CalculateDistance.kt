package com.example.i_radar

import android.location.Location

object CalculateDistance {

    fun calculateDistanceAndBearing(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Pair<Float, Float> {
        val results = FloatArray(2)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return Pair(results[0], results[1])
    }
}
