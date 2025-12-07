package com.example.i_radar

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.i_radar.MainActivity.NavigationResult
import com.example.i_radar.R

    private val visibleLines = mutableListOf<String>() // 👈 store currently visible lines

    private var currentDegree = 0f  // <-- declare here

fun showCompasArrow(
    activity: Activity,
    fullLocationsList: List<NavigationResult>,
    azimuth: Float,
) {
    val arrowStatic = false

    // ✅ Use raw azimuth (no smoothing)
    val displayAzimuth = azimuth

    // Normalize to 0–360
    val normalizedAzimuth = (displayAzimuth + 360) % 360

    // Update direction text

    if (arrowStatic) {
        val arrow = activity.findViewById<ImageView>(R.id.directionArrow)
        arrow.rotation = -normalizedAzimuth   // instant
    } else {
        val compassBackground = activity.findViewById<ImageView>(R.id.compassBackground)

        // Calculate shortest rotation path
        val delta = ((normalizedAzimuth - currentDegree + 540) % 360) - 180
        val targetRotation = currentDegree + delta

        compassBackground.rotation = -targetRotation   // instant

        currentDegree = targetRotation
    }
}

private val previousBearings = mutableMapOf<String, Float>()
private val smoothedBearings = mutableMapOf<String, Float>()
private const val MARKER_ALPHA = 0.2f // lower = smoother

fun showPointsOnCompas(
    activity: Activity,
    fullLocationsList: List<NavigationResult>,
    azimuth: Float
) {
    val markerView = activity.findViewById<AzimuthMarkerView>(R.id.azimuthMarker)

    val visibleLocationsList = if (visibleLines.isEmpty()) {
        fullLocationsList
            .sortedBy { it.distance }
            .take(5)
    } else {
        fullLocationsList
            .filter { r ->
                visibleLines.any { line -> line.contains(r.point.name.take(14)) }
            }
            .sortedBy { it.distance }
    }

    val markerList = visibleLocationsList.map { r ->
        var bearingToDisplay = (r.bearing - azimuth + 360f) % 360f

        // --- Smooth bearing ---
        val key = r.point.name
        val lastBearing = smoothedBearings[key] ?: bearingToDisplay
        val delta = ((bearingToDisplay - lastBearing + 540f) % 360f) - 180f // shortest path
        val smoothed = (lastBearing + MARKER_ALPHA * delta + 360f) % 360f
        smoothedBearings[key] = smoothed

        Marker(
            azimuth = smoothed,
            color = MarkerColor.colors[r.index % MarkerColor.colors.size],
            radius = 100f,
            drawAtCenter = r.atPoint,
            distance = r.distance.toInt()
        )
    }

    // ✅ Update marker view
    markerView.setMarkers(markerList)
}



    fun showPointsOnList(activity: Activity,fullLocationsList: List<NavigationResult>) {

        val pointsContainer = activity.findViewById<LinearLayout>(R.id.pointsContainer)
        pointsContainer.removeAllViews()

        for ( point in fullLocationsList) {
//            val sdf = SimpleDateFormat("dd:MM:yyyy HH:mm:ss", Locale.getDefault())
//            val lastUpdateStr = point.point.lastUpdate?.let { sdf.format(it) } ?: "N/A"
            val tv = TextView(activity)
            tv.textSize = 20f
            tv.setTypeface(null, Typeface.BOLD) // 👈 makes the text bold

            val arrowChar = if (point.isUpToDate) '▲' else '▼'

            if (arrowChar == '▲') {
                tv.setTextColor(Color.GREEN)
            } else {
                tv.setTextColor(Color.BLACK)
            }

// Combine the arrow into the formatted string
            val text = String.format(
                "%-14s %-6d %c",
                point.point.name.take(14),
                point.distance.toInt(),
                arrowChar
            )

            tv.text = text
            tv.typeface = Typeface.MONOSPACE // ensures columns align


            tv.setPadding(16, 16, 16, 16)
            // Set background color, cycling through list if more points than colors
            tv.setBackgroundColor(MarkerColor.colors[point.index % MarkerColor.colors.size])
            pointsContainer.addView(tv)
        }
    }


    // 🧩 helper function to update visible lines
    fun updateVisibleLines(activity: Activity,scrollView: ScrollView, container: LinearLayout, fullLocationsList: List<NavigationResult>) {
        val scrollY = scrollView.scrollY
        val scrollHeight = scrollView.height

        visibleLines.clear()

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val childTop = child.top
            val childBottom = child.bottom

            if (childBottom > scrollY && childTop < scrollY + scrollHeight) {
                val tv = child as TextView
                visibleLines.add(tv.text.toString())
            }
        }

        // Example: show in log or use in other parts of app
        Log.d("VisibleLines", "Currently visible: $visibleLines")

    }

