package com.example.i_radar  // <-- use your actual package name

import android.util.Log
import com.example.i_radar.MainActivity.Companion.groupId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    fun readAllLocations(onResult: (List<ReferencePoint>) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection(groupId)
            .get()
            .addOnSuccessListener { documents ->
                val referencePoints = mutableListOf<ReferencePoint>()

                for (doc in documents) {
                    val name = doc.id
                    val latitude = doc.getDouble("latitude")
                    val longitude = doc.getDouble("longitude")

// Read lastUpdate timestamp
                    val lastUpdate = doc.getTimestamp("lastUpdate")?.toDate() // converts to java.util.Date

// Read requestUpdate boolean
                    val requestUpdate = doc.getBoolean("requestUpdate") ?: false

// Read requestFrom array
                    val requestFrom = doc.get("requestFrom") as? List<String> ?: emptyList()

                    Log.d(
                        "Firestore",
                        "Document: $name, Latitude: $latitude, Longitude: $longitude, " +
                                "LastUpdate: $lastUpdate, RequestUpdate: $requestUpdate, RequestFrom: $requestFrom"
                    )

                    if (latitude != null && longitude != null) {
                        referencePoints.add(ReferencePoint(name, latitude, longitude, lastUpdate, requestUpdate, requestFrom))
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
            "lastUpdate" to Timestamp.now(),          // current server timestamp
            "requestUpdate" to false,                 // boolean field
            "requestFrom" to listOf<String>()        // empty array
        )

        db.collection("locations")
            .document(point.name)
            .set(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Wrote location: ${point.name}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error writing location", e)
                onComplete(false)
            }
    }

    fun deleteLocation(userName: String, onComplete: (Boolean) -> Unit) {

        val db = FirebaseFirestore.getInstance()

        db.collection("locations").document(userName)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Document successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting document", e)
            }
    }


}
