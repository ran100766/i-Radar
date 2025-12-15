package com.example.i_radar

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.i_radar.MainActivity.Companion.userGroupId

/**
 * Handles the sequential loading and setup of user data (name and group).
 */
class UserDataManager(private val activity: AppCompatActivity) {

    private val prefs = activity.getSharedPreferences("i-radar-prefs", Context.MODE_PRIVATE)

    fun initializeUserData() {
        // Start the process by always asking for the username.
        ensureUserNameExists {
            // Once the username is confirmed, always ask for the group choice.
            ensureGroupIdExists()
        }
    }

    /**
     * STEP 1: Always ask for the user name, suggesting the saved one if it exists.
     */
    fun ensureUserNameExists(onComplete: () -> Unit) {
        val savedName = prefs.getString("userName", null)

        // Always ask for the user name, suggesting the saved one.
        askForUserName(activity, suggestedName = savedName) { name ->
            MainActivity.userName = name
            prefs.edit().putString("userName", name).apply()
            Log.d("UserData", "User name confirmed/updated to: $name")
            onComplete() // Proceed to the next step
        }
    }

    /**
     * STEP 2: Always ask the user to join or create a group.
     */
    fun ensureGroupIdExists() {
        // Always ask the user to choose a group.
        // The dialog will intelligently suggest saved values from SharedPreferences.
        askForGroupChoice(activity) { groupId ->
            MainActivity.userGroupId = groupId
            prefs.edit().putString("userGroupId", groupId).apply()

            // If a new group was created, its name is passed back and saved.
            if (userGroupId != null) {
                MainActivity.userGroupId = groupId
                Log.d("UserData", "New group created. Group ID: $groupId ")
            } else {
                // When joining, the groupName is null. We will load the real one from Firestore later.
                Log.d("UserData", "Joined/Confirmed existing group. Group ID: $groupId")
            }
        }
    }
}
