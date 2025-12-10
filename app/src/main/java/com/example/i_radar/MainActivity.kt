package com.example.i_radar

import CompassManager
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import java.util.Date

private const val FIVE_MINUTES_MS = 5 * 60 * 1000

class MainActivity : AppCompatActivity() {
    companion object {
        val noName = "No_Name"

        val defaultGroupId = "RANGROUP17"
        var userName: String = noName

        var userGroupId: String = defaultGroupId

        var groupName: String = noName

    }

    private lateinit var compassManager: CompassManager

    public var fullLocationsList: List<NavigationResult> = emptyList()

    private lateinit var tvGroupKey: TextView
    public lateinit var tvUserName: TextView
    private lateinit var tmMembersOnline: TextView
    private lateinit var tmMembersOffline: TextView
    private val uiUpdateHandler = Handler(Looper.getMainLooper())

    // Counters
    private var upToDateCount = 0
    private var outDatedCount = 0


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
        var isUpToDate: Boolean = false,
        var index: Int = 0
    )

    private var referencePoints: MutableList<ReferencePoint> = mutableListOf()

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
        setContentView(R.layout.activity_main)

        val arrowImage = findViewById<ImageView>(R.id.compassCircle)
        arrowImage.setImageResource(R.drawable.circle)

        compassManager = CompassManager(this) { azimuth ->
            smoothedAzimuth = smoothAzimuth(smoothedAzimuth, azimuth)

            showCompasArrow(this, fullLocationsList, smoothedAzimuth)
            showPointsOnCompas(this, fullLocationsList, smoothedAzimuth)
        }


        tvGroupKey = findViewById(R.id.tvGroupKey)
        tvUserName = findViewById(R.id.tvUserName)
        tmMembersOnline = findViewById(R.id.tvMembersOnline)
        tmMembersOffline = findViewById(R.id.tvMembersOffline)

        FirebaseApp.initializeApp(this)

        // Use the new manager to handle user data loading and setup.
        UserDataManager(this).initializeUserData()

        locationPermissionRequest.launch(locationPermissions)
        requestIgnoreBatteryOptimizations()


        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val pointsContainer = findViewById<LinearLayout>(R.id.pointsContainer)

        // Add a scroll listener
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            updateVisibleLines(this, scrollView, pointsContainer, fullLocationsList)
        }

        tvGroupKey.setOnClickListener {
            copyToClipboard(userGroupId)
        }
    }


    private fun updateUI(location: Location) {
        val speedMps = location.speed
        val speedKmh = speedMps * 3.6
        val speedKnots = speedMps * 1.94384

        printHeader()

        val firestoreManager = FirestoreManager()

        // Corrected callback to handle a single list of points
        firestoreManager.readAllLocations { points: List<ReferencePoint> ->
            if (points.isNotEmpty()) {
                referencePoints = points.toMutableList()
            }
        }

        val now = Date()
        upToDateCount = 0
        outDatedCount = 0

        fullLocationsList = referencePoints.map { point ->

            // Check recency
            val isUpToDate = point.lastUpdate?.let {
                now.time - it.time <= FIVE_MINUTES_MS
            } ?: false   // if null → treat as not recent

            // Count
            if (isUpToDate) upToDateCount++ else outDatedCount++

            // Calculate distance & bearing
            val (distance, bearing) = CalculateDistance.calculateDistanceAndBearing(
                location.latitude,
                location.longitude,
                point.lat,
                point.lon
            )

            NavigationResult(point, distance, bearing, distance < 10F, isUpToDate)

        }.sortedBy { it.distance }
            .onEachIndexed { index, result ->
                result.index = index
            }

        showPointsOnList(this, fullLocationsList)
    }

    private fun printHeader() {
        tvGroupKey.text = "Group: $groupName"
        tvUserName.text = userName
         tmMembersOnline.text = "Online: $upToDateCount ▲"
         tmMembersOffline.text = "Offline: $outDatedCount ▼"
    }


    override fun onStart() {
        super.onStart()
        uiUpdateHandler.post(uiUpdateRunnable) // start periodic updates
    }

    override fun onStop() {
        super.onStop()
        uiUpdateHandler.removeCallbacks(uiUpdateRunnable) // stop updates when activity stops
    }

    override fun onResume() {
        super.onResume()
        compassManager.start()
    }

    override fun onPause() {
        super.onPause()
        compassManager.stop()
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

    // Function to copy text
    fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Group Key", text))
        Toast.makeText(this, "Group key copied: " + text, Toast.LENGTH_SHORT).show()
    }

}
