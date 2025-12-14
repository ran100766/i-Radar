package com.example.i_radar

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.i_radar.MainActivity.Companion.defaultGroupId
import com.example.i_radar.MainActivity.Companion.noName

/**
 * PUBLIC: Asks the user for their name.
 * It will suggest a previously saved name if one is provided.
 */
fun askForUserName(
    activity: Activity,
    suggestedName: String?,
    onNameDone: (String) -> Unit
) {
    val nameInput = EditText(activity).apply {
        val currentName = suggestedName ?: ""
        setText(currentName)
        setSelection(currentName.length)
    }

    val dialog = AlertDialog.Builder(activity)
        .setTitle("Your Name")
        .setMessage("Please enter your name:")
        .setView(nameInput)
        .setCancelable(false)
        .setPositiveButton("OK", null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            var name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                name = noName // Use default if empty
            }
            dialog.dismiss()
            onNameDone(name)
        }
    }
    dialog.show()
}

/**
 * PUBLIC: Asks the user to choose how to set up their group.
 */
fun askForGroupChoice(
    activity: Activity,
    onGroupInfoEntered: (groupId: String, groupName: String?) -> Unit
) {
    val prefs = activity.getSharedPreferences("i-radar-prefs", Context.MODE_PRIVATE)
    val savedGroupId = prefs.getString("userGroupId", null)
    val savedGroupName = prefs.getString("groupName", null)

    val options = mutableListOf<String>()

    Log.d("Saved_Groups", "Saved_Groups: $savedGroupId  $savedGroupName .")


    // Only show the 'Join last group' option if a valid group is saved.
    if (savedGroupId != null && savedGroupName != null && savedGroupName != noName) {
        options.add("Join last group ($savedGroupName)")
    }
    options.add("Join other existing group")
    options.add("Create new group")

    AlertDialog.Builder(activity)
        .setTitle("Group Setup")
        .setItems(options.toTypedArray()) { _, which ->
            val selectedOption = options[which]
            when {
                selectedOption.startsWith("Join last group") -> {
                    // This is only shown if savedGroupId and savedGroupName are valid.
                    onGroupInfoEntered(savedGroupId!!, savedGroupName)
                }
                selectedOption == "Join other existing group" -> {
                    askToJoinOtherGroup(activity, onGroupInfoEntered)
                }
                selectedOption == "Create new group" -> {
                    askToCreateNewGroup(activity, onGroupInfoEntered)
                }
            }
        }
        .setCancelable(false)
        .show()
}

/**
 * PRIVATE HELPER: Handles the 'Create New Group' flow.
 */
private fun askToCreateNewGroup(
    activity: Activity,
    onComplete: (groupId: String, groupName: String) -> Unit
) {
    val newGroupId = generateRandomGroupId()
    val nameInput = EditText(activity).apply {
        hint = "My Awesome Group"
    }

    val dialog = AlertDialog.Builder(activity)
        .setTitle("Create New Group")
        .setMessage("Your new Group ID is: $newGroupId\n\nPlease give your group a name:")
        .setView(nameInput)
        .setCancelable(false)
        .setPositiveButton("OK", null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val groupName = nameInput.text.toString().trim()
            if (groupName.isEmpty()) {
                Toast.makeText(activity, "Group name cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                // Save the new group to Firestore first.
                val firestoreManager = FirestoreManager()
                firestoreManager.createNewGroup(newGroupId, groupName) { success ->
                    if (success) {
                        Log.d("Firestore", "New group $groupName ($newGroupId) saved to Firestore.")
                        // Then, pass the data back to the UserDataManager.
                        onComplete(newGroupId, groupName)
                    } else {
                        Toast.makeText(activity, "Error: Could not create group.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    dialog.show()
}

/**
 * PRIVATE HELPER: Asks for a new group ID to join, with no suggestions.
 */
private fun askToJoinOtherGroup(
    activity: Activity,
    onComplete: (groupId: String, groupName: String?) -> Unit
) {
    val groupInput = EditText(activity).apply {
        hint = "Enter or paste Group ID"
    }

    val dialog = AlertDialog.Builder(activity)
        .setTitle("Join Other Group")
        .setMessage("Please enter the Group ID to join:")
        .setView(groupInput)
        .setCancelable(false)
        .setPositiveButton("OK", null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val groupId = groupInput.text.toString().trim()
            if (groupId.isEmpty()) {
                Toast.makeText(activity, "Group ID cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                onComplete(groupId, null) // Name is unknown, pass null
            }
        }
    }
    dialog.show()
}

/**
 * HELPER: Generates a random ID for creating a new group.
 */
private fun generateRandomGroupId(length: Int = 16): String {
    val allowedChars = ('A'..'Z')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}
