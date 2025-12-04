package com.example.i_radar  // <-- use your actual package name

import android.util.Log
import com.example.i_radar.MainActivity.Companion.userGroupId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    fun readAllLocations(onResult: (List<ReferencePoint>) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("groups")
            .document(userGroupId)
            .collection("devices")
            .get()
            .addOnSuccessListener { documents ->
                val referencePoints = mutableListOf<ReferencePoint>()

                for (doc in documents) {
                    val name = doc.getString("name") ?: doc.id
                    val latitude = doc.getDouble("latitude")
                    val longitude = doc.getDouble("longitude")

                    // Read lastUpdate timestamp (Firestore Timestamp → java.util.Date)
                    val lastUpdate = doc.getTimestamp("lastUpdate")?.toDate()

                    Log.d(
                        "Firestore",
                        "Device: $name, Lat=$latitude, Lon=$longitude, LastUpdate=$lastUpdate"
                    )

                    if (latitude != null && longitude != null) {
                        referencePoints.add(
                            ReferencePoint(name, latitude, longitude,  lastUpdate)
                        )
                    }
                }

                onResult(referencePoints)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error reading locations", e)
                onResult(emptyList())
            }
    }

    fun writeLocation(point: ReferencePoint, onComplete: (Boolean) -> Unit) {
        val data = hashMapOf(
            "latitude" to point.lat,
            "longitude" to point.lon,
            "lastUpdate" to Timestamp.now(),
            "name" to point.name
        )

        val db = FirebaseFirestore.getInstance()

        db.collection("groups")
            .document(userGroupId)             // <-- group ID (same for all devices)
            .collection("devices")
            .document(point.name)              // <-- device_id
            .set(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Wrote device ${point.name}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error writing location", e)
                onComplete(false)
            }
    }


}
