package com.example.i_radar

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.i_radar.ReferencePoint
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.example.i_radar.FirestoreManager
import com.example.i_radar.CalculateDistance.calculateDistanceAndBearing
import com.example.i_radar.MainActivity.Companion.noName
import com.example.i_radar.MainActivity.Companion.userName
import com.google.firebase.Timestamp

class LocationService : Service() {

    companion object {
        var latestLocation: Location = Location("custom").apply {
            latitude = 90.0
            longitude = 180.0
        }
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        )
            .setMinUpdateDistanceMeters(5f) // 🔹 only if moved 2 meters
            .setMinUpdateIntervalMillis(5_000L) // don’t trigger faster than 5 sec
            .build()

        createNotificationChannel()
        startForeground(1, createNotification("Tracking location..."))

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {

            // ✅ Use only the latest location
            result.lastLocation?.let { location ->

                val distance = calculateDistanceAndBearing(
                    location.latitude,
                    location.longitude,
                    latestLocation.latitude,
                    latestLocation.longitude
                ).first

                Log.d("DistanceCheck", "Distance: $distance meters")

                if (/*abs(distance) > 2.0 && */userName != noName) {
                    val myLocation = ReferencePoint(userName, location.latitude, location.longitude, Timestamp.now().toDate(),false,listOf<String>() )
                    FirestoreManager().writeLocation(myLocation) { success ->
                        if (success) Log.d("Main", "Location saved!")
                    }
                    Log.d(
                        "Firestore",
                        "Document write: $userName, Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                    )

                }
                latestLocation = location
            }
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("GPS Compass")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("LocationService", "Service destroyed, updates stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Stop foreground service
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates() // call the function
        return START_NOT_STICKY // <-- here, return type is Int, correct
    }

}
