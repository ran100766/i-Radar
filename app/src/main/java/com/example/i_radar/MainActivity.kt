package com.example.i_radar

import CompassManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import com.google.firebase.FirebaseApp
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import java.util.Date

private const val FIVE_MINUTES_MS = 5 * 60 * 1000

class MainActivity : AppCompatActivity() {
    companion object {
        val noName = "No_Name"

        val defaultGroupId = "RANGROUP17"
        var userName: String = noName

        var userGroupId: String = defaultGroupId

    }

    private lateinit var compassManager: CompassManager

    public var fullLocationsList: List<NavigationResult> = emptyList()

    private lateinit var tvGroupKey: TextView
    public lateinit var tvGroupDescription: TextView
    private lateinit var tmMembersOnline: TextView
    private lateinit var tmMembersOffline: TextView
    private val uiUpdateHandler = Handler(Looper.getMainLooper())

    // Counters
    private var recentCount = 0
    private var nonRecentCount = 0


    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            // Read latest location from service
            val location = LocationService.latestLocation
            location?.let {
                updateUI(location)
            }

            // Schedule next update in 2 seconds
            uiUpdateHandler.postDelayed(this, 5000)
        }
    }

    data class NavigationResult(
        var point: ReferencePoint,
        var distance: Float,
        var bearing: Float,
        var atPoint: Boolean = false,
        var index: Int = 0,
    )

    private var referencePoints: MutableList<ReferencePoint> = mutableListOf(
//        ReferencePoint("Jerusalem", 31.7795, 35.2339),
//        ReferencePoint("Home", 32.17062, 34.83878),
//        ReferencePoint("Marina", 32.16580, 34.79267)
    )

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                // ✅ Permission granted → start the location service
                startLocationService()
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    var smoothedAzimuth = 0f
    val smoothingFactor = 0.1f  // smaller = smoother


    fun smoothAzimuth(oldAzimuth: Float, newAzimuth: Float): Float {
        var delta = newAzimuth - oldAzimuth

        // Handle wrap-around: keep delta between -180° and +180°
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360

        // Apply low-pass filter
        val result = (oldAzimuth + smoothingFactor * delta + 360) % 360
        return result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // must match activity_main.xml

        val arrowImage = findViewById<ImageView>(R.id.compassCircle)
        arrowImage.setImageResource(R.drawable.circle)

        compassManager = CompassManager(this) { azimuth ->
            smoothedAzimuth = smoothAzimuth(smoothedAzimuth, azimuth)

            showCompasArrow(this, fullLocationsList, smoothedAzimuth)
            showPointsOnCompas(this, fullLocationsList, smoothedAzimuth)
        }


        tvGroupKey = findViewById(R.id.tvGroupKey)
        tvGroupDescription = findViewById(R.id.tvGroupDescription)
        tmMembersOnline = findViewById(R.id.tvMembersOnline)
        tmMembersOffline = findViewById(R.id.tvMembersOffline)

        FirebaseApp.initializeApp(this)

// Save name
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        prefs.edit().putString("userName", userName).apply()

// Load name in onCreate
        val savedName = prefs.getString("userName", null)
        if (savedName == null || savedName == noName) {
            askUserNameAndGroup(this) { name, groupId ->
                userName = name
                userGroupId = groupId
                prefs.edit().putString("userName", name).apply()
                Log.d("UserData", "Name: $name, Group ID: $userGroupId")
                // Store or use the name and groupId as needed
            }
        } else {
            userName = savedName
        }

        locationPermissionRequest.launch(locationPermissions)
        requestIgnoreBatteryOptimizations()


        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val pointsContainer = findViewById<LinearLayout>(R.id.pointsContainer)

// Add a scroll listener
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            updateVisibleLines(this, scrollView, pointsContainer, fullLocationsList)
        }
    }


    private fun updateUI(location: Location) {
        val speedMps = location.speed
        val speedKmh = speedMps * 3.6
        val speedKnots = speedMps * 1.94384

        printHeader()
//        tvSpeed.text = "Speed: %.1f knots".format(speedKnots)
//        tvLatitude.text = "Lat: %.5f".format(location.latitude)
//        tvLongitude.text = "Lng: %.5f".format(location.longitude)


        val firestoreManager = FirestoreManager()

        firestoreManager.readAllLocations { points ->
            // This block runs after Firestore data is loaded
            if (points.isNotEmpty()) {
                // Assign to a variable for later use
                referencePoints = points.toMutableList()

                // Use referencePoints here, e.g., update UI or show on map
            }
        }

// 5 minutes in milliseconds
        val now = Date()
        recentCount = 0
        nonRecentCount = 0

        fullLocationsList = referencePoints.map { point ->

            // Check recency
            val isRecent = point.lastUpdate?.let {
                now.time - it.time <= FIVE_MINUTES_MS
            } ?: false   // if null → treat as not recent

            // Count
            if (isRecent) recentCount++ else nonRecentCount++

            // Calculate distance & bearing
            val (distance, bearing) = CalculateDistance.calculateDistanceAndBearing(
                location.latitude,
                location.longitude,
                point.lat,
                point.lon
            )

            NavigationResult(point, distance, bearing, distance < 10F)

        }.sortedBy { it.distance }
            .onEachIndexed { index, result ->
                result.index = index
            }

        showPointsOnList(this, fullLocationsList)
    }

    private fun printHeader() {
        tvGroupKey.text = userGroupId
        tvGroupDescription.text = userName + "@" + userGroupId
        tmMembersOnline.text = "Online: " + recentCount
        tmMembersOffline.text = "Offline: " + nonRecentCount
    }


    override fun onStart() {
        super.onStart()
        uiUpdateHandler.post(uiUpdateRunnable) // start periodic updates
    }

    override fun onStop() {
        super.onStop()
        uiUpdateHandler.removeCallbacks(uiUpdateRunnable) // stop updates when activity stops
    }
//    private var updateJob: Job? = null

    override fun onResume() {
        super.onResume()
        compassManager.start()

//        updateJob = lifecycleScope.launch {
//            while (isActive) {
//                showPointsOnCompas(this@MainActivity, fullLocationsList)
//                delay(500)
//            }
//        }
    }

    override fun onPause() {
        super.onPause()
        compassManager.stop()
//        updateJob?.cancel()

    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}
