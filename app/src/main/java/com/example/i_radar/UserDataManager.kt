package com.example.i_radar

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Handles loading, saving, and setting up user data like name and group ID
 * using SharedPreferences.
 */
class UserDataManager(context: Context) {

    private val prefs = context.getSharedPreferences("i-radar-prefs", Context.MODE_PRIVATE)

    /**
     * Checks for saved user data. If not found, prompts the user to enter it.
     * Otherwise, loads the saved data into MainActivity.
     */
    fun initializeUserData(activity: AppCompatActivity) {
        val savedName = prefs.getString("userName", null)
        val savedGroupId = prefs.getString("userGroupId", null)
        val savedGroupName = prefs.getString("groupName", null)

        // If name or group ID is missing, we need to ask the user for initial setup.
        if (savedName == null || savedName == MainActivity.noName || savedGroupId == null) {

            // Prompt the user to enter their details.
            askUserNameAndGroup(activity) { name, groupId, groupName ->
                // Update the running state in MainActivity's companion object
                MainActivity.userName = name
                MainActivity.userGroupId = groupId
                // Note: groupName is fetched from Firestore later, not set here.

                // Save the new details for the next session.
                prefs.edit().apply {
                    putString("userName", name)
                    putString("userGroupId", groupId)
                    apply()
                }
                Log.d("UserData", "Initial setup complete. Name: $name, Group ID: $groupId")
            }

        } else {
            // If data exists, load it into MainActivity's companion object.
            MainActivity.userName = savedName
            MainActivity.userGroupId = savedGroupId
            MainActivity.groupName = savedGroupName ?: "No_Name" // Use default if null

            Log.d("UserData", "Loaded from Prefs → Name: ${MainActivity.userName}, Group Name: ${MainActivity.groupName}, Group ID: ${MainActivity.userGroupId}")
        }
    }
}
