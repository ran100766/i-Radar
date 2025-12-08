package com.example.i_radar  // <-- use your actual package name

import android.util.Log
import com.example.i_radar.MainActivity.Companion.userGroupId
import com.example.i_radar.MainActivity.Companion.groupName
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    fun readAllLocations(onResult: (List<ReferencePoint>) -> Unit) {
        val groupDocRef = db.collection("groups").document(userGroupId)

        // 1. Get the main group document to read its fields (like 'group_name')
        groupDocRef.get()
            .addOnSuccessListener { groupDocument ->
                groupName = groupDocument.getString("groupName") ?: "No Name"
                Log.d("Firestore", "Successfully read group name: $groupName")

                // 2. Then, get the devices in the subcollection
                groupDocRef.collection("devices")
                    .get()
                    .addOnSuccessListener { devicesSnapshot ->
                        val referencePoints = mutableListOf<ReferencePoint>()
                        for (doc in devicesSnapshot) {
                            val name = doc.getString("name") ?: doc.id
                            val latitude = doc.getDouble("latitude")
                            val longitude = doc.getDouble("longitude")
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
                        // Success: return both group name and the list of points
                        onResult(referencePoints)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error reading 'devices' subcollection", e)
                        // Failure: return group name but an empty list for points
                        onResult(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error reading group document", e)
                // If we can't read the group doc, we can't get anything.
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
