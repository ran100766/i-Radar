package com.example.i_radar

fun formatDistance(distanceMeters: Int): String {
    return when {
        distanceMeters < 10_000 -> {
            // < 10 km → meters
            "${distanceMeters}m"
        }

        distanceMeters < 100_000 -> {
            // 10–100 km → km with 2 decimals
            val km = distanceMeters / 1000.0
            String.format("%.1fkm", km)
        }

        else -> {
            // ≥ 100 km → km, no decimals
            val km = distanceMeters / 1000
            "${km}km"
        }
    }
}
