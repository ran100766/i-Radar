package com.example.i_radar

import android.util.Log
import com.example.i_radar.MainActivity.Companion.defaultGroupId
import com.example.i_radar.MainActivity.Companion.userGroupId
import com.example.i_radar.MainActivity.Companion.groupName
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions

class FirestoreManager {

    var isGroupJoined = false

    private val db = FirebaseFirestore.getInstance()

    init {
        // Sign in anonymously when the manager is created
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener {
                Log.d("FirestoreAuth", "Signed in anonymously with UID: ${it.user?.uid}")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreAuth", "Anonymous sign-in failed", e)
            }
    }
    fun createNewGroup(
        groupId: String,
        groupName: String,
        onComplete: (Boolean) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().uid
        if (uid == null) {
            onComplete(false)
            return
        }

        val db = FirebaseFirestore.getInstance()

        val groupData = hashMapOf(
            "groupName" to groupName,
            "ownerUid" to uid,                 // ✅ REQUIRED by rules
            "createdAt" to Timestamp.now()
        )

        // 1️⃣ Create the group document (allowed by rules)
        db.collection("groups")
            .document(groupId)
            .set(groupData)
            .addOnSuccessListener {

                // 2️⃣ STEP 6 — creator joins group via central logic
                joinGroup(groupId) { joined ->
                    if (joined) {
                        Log.d("Firestore", "Group $groupName created and user joined as owner")
                        onComplete(true)
                    } else {
                        Log.e("Firestore", "Group created but failed to join as member")
                        onComplete(false)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error creating group document", e)
                onComplete(false)
            }
    }

    fun readAllLocations(onResult: (List<ReferencePoint>) -> Unit) {

        // 🔐 STEP 6 — ensure user is a group member BEFORE reading
        ensureGroupJoined(userGroupId) { joined ->
            if (!joined) {
                Log.e("Firestore", "User is not a member of group $userGroupId")
                onResult(emptyList())
                return@ensureGroupJoined
            }

            val groupDocRef = db.collection("groups").document(userGroupId)

            // 1️⃣ Read group document (allowed now)
            groupDocRef.get()
                .addOnSuccessListener { groupDocument ->
                    groupName = groupDocument.getString("groupName") ?: defaultGroupId
                    Log.d("Firestore", "Successfully read group name: $groupName")

                    // 2️⃣ Read devices subcollection
                    groupDocRef.collection("devices")
                        .get()
                        .addOnSuccessListener { devicesSnapshot ->
                            val referencePoints = mutableListOf<ReferencePoint>()

                            for (doc in devicesSnapshot) {
                                val name = doc.getString("name") ?: doc.id
                                val latitude = doc.getDouble("latitude")
                                val longitude = doc.getDouble("longitude")
                                val lastUpdate = doc.getTimestamp("lastUpdate")?.toDate()

                                if (latitude != null && longitude != null) {
                                    referencePoints.add(
                                        ReferencePoint(
                                            name,
                                            latitude,
                                            longitude,
                                            lastUpdate
                                        )
                                    )
                                }
                            }

                            onResult(referencePoints)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error reading devices", e)
                            onResult(emptyList())
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error reading group document", e)
                    onResult(emptyList())
                }
        }
    }

    fun writeLocation(point: ReferencePoint, onComplete: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("Firestore", "User not authenticated")
            onComplete(false)
            return
        }

        val uid = user.uid
        val displayName = point.name   // 🔥 document ID = displayName

        val deviceDocRef = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(userGroupId)
            .collection("devices")
            .document(displayName) // ✅ SAME AS C++

        val data = hashMapOf(
            "latitude" to point.lat,
            "longitude" to point.lon,
            "lastUpdate" to Timestamp.now(),
            "name" to displayName,
            "ownerUid" to uid           // ✅ REQUIRED BY RULES
        )

        deviceDocRef
            .set(data, SetOptions.merge()) // PATCH-like behavior
            .addOnSuccessListener {
                Log.d("Firestore", "Device $displayName written successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to write device", e)
                onComplete(false)
            }
    }

    fun joinGroup(groupId: String, onComplete: (Boolean) -> Unit) {
        val uid = FirebaseAuth.getInstance().uid ?: return

        val memberData = hashMapOf(
            "joinedAt" to Timestamp.now()
        )

        FirebaseFirestore.getInstance()
            .collection("groups")
            .document(groupId)
            .collection("members")
            .document(uid)          // 👈 THIS IS STEP 6
            .set(memberData)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }


    private fun ensureGroupJoined(
        groupId: String,
        onReady: (Boolean) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().uid
        if (uid == null) {
            onReady(false)
            return
        }

        db.collection("groups")
            .document(groupId)
            .collection("members")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // Already a member
                    onReady(true)
                } else {
                    // Join group (STEP 6)
                    joinGroup(groupId, onReady)
                }
            }
            .addOnFailureListener {
                onReady(false)
            }
    }

}
