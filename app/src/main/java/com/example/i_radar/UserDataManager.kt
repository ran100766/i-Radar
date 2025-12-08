package com.example.i_radar

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Handles the sequential loading and setup of user data (name and group).
 */
class UserDataManager(private val activity: AppCompatActivity) {

    private val prefs = activity.getSharedPreferences("i-radar-prefs", Context.MODE_PRIVATE)

    fun initializeUserData() {
        // Start the process by ensuring we have a username.
        ensureUserNameExists {
            // Once the username is confirmed, ensure a group ID exists.
            ensureGroupIdExists()
        }
    }

    /**
     * STEP 1: Check for a saved user name. If not found, ask the user for it.
     */
    private fun ensureUserNameExists(onComplete: () -> Unit) {
        val savedName = prefs.getString("userName", null)

        if (savedName == null || savedName == MainActivity.noName)
        {
            // No valid name saved, so we must ask for it.
            askForUserName(activity, suggestedName = null) { name ->
                MainActivity.userName = name
                prefs.edit().putString("userName", name).apply()
                Log.d("UserData", "User name set to: $name")
                onComplete() // Proceed to the next step (checking for group ID)
            }
        } else {
            // A valid name is already saved. Load it and proceed.
            MainActivity.userName = savedName
            Log.d("UserData", "Loaded user name: $savedName")
            onComplete() // Proceed to the next step
        }
    }

    /**
     * STEP 2: Check for a saved group ID. If not found, ask the user to join/create.
     */
    private fun ensureGroupIdExists() {
        val savedGroupId = prefs.getString("userGroupId", null)

        MainActivity.groupName = prefs.getString("groupName", MainActivity.noName) ?: MainActivity.noName

        if (savedGroupId == null || MainActivity.groupName == MainActivity.noName) {
            // No group ID is saved, so ask the user to choose.
            askForGroupChoice(activity) { groupId ->
                MainActivity.userGroupId = groupId
                prefs.edit().putString("userGroupId", groupId).apply()
                Log.d("UserData", "User group set to: $groupId")
            }
        } else {
            // Group ID is already saved. Load it.
            MainActivity.userGroupId = savedGroupId
            // Also load the associated group name, if it exists.
            Log.d("UserData", "Loaded group ID: $savedGroupId, Group Name: ${MainActivity.groupName}")
        }
    }
}
