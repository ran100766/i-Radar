package com.example.i_radar

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.i_radar.MainActivity.Companion.userGroupId

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
        .setCancelable(true)
        .setPositiveButton("OK", null)
        .setNegativeButton("Cancel", null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            var name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                name = MainActivity.noName // Use default if empty
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
    onGroupInfoEntered: (groupId: String) -> Unit
) {
    val prefs = activity.getSharedPreferences("i-radar-prefs", Context.MODE_PRIVATE)
    val savedGroupId = prefs.getString("userGroupId", null)
    val savedGroupName = prefs.getString("groupName", null)

    Log.d("SavedPref", "Last group name: $savedGroupName; Last group id: $savedGroupId.")

    val options = mutableListOf<String>()

    // Only show the 'Join last group' option if a valid group is saved.
    if (savedGroupId != null && savedGroupName != null) {
        options.add("Join last group: $savedGroupId")
    }
    options.add("Join other existing group")
    options.add("Create new group")

    val layoutInflater = LayoutInflater.from(activity)
    val customView = layoutInflater.inflate(R.layout.dialog_group_choice, null)

    val groupNameTextView = customView.findViewById<TextView>(R.id.current_group_name)
    val optionsListView = customView.findViewById<ListView>(R.id.group_options_list)

    val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, options)
    optionsListView.adapter = adapter

    val dialog = AlertDialog.Builder(activity)
        .setTitle("Group Setup")
        .setView(customView)
        .setNegativeButton("Cancel", null)
        .create()

    optionsListView.setOnItemClickListener { _, _, position, _ ->
        val selectedOption = options[position]
        when {
            selectedOption.startsWith("Join last group") -> {
                onGroupInfoEntered(savedGroupId!!)
            }
            selectedOption == "Join other existing group" -> {
                askToJoinOtherGroup(activity, onGroupInfoEntered)
            }
            selectedOption == "Create new group" -> {
                askToCreateNewGroup(activity, onGroupInfoEntered)
            }
        }
        dialog.dismiss()
    }


    dialog.show()
}

/**
 * PRIVATE HELPER: Handles the 'Create New Group' flow.
 */
private fun askToCreateNewGroup(
    activity: Activity,
    onComplete: (groupId: String) -> Unit
) {
    val newGroupId = generateRandomGroupId()
    val nameInput = EditText(activity).apply {
        hint = "    "
    }

    val dialog = AlertDialog.Builder(activity)
        .setTitle("New Group key:               ")
        .setMessage("$newGroupId\n\n\n\nPlease enter a group name:")
        .setView(nameInput)
        .setCancelable(true)
        .setPositiveButton("OK", null)
        .setNegativeButton("Cancel", null)
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
                        copyToClipboard(activity, newGroupId)
                        onComplete(newGroupId)
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
    onComplete: (groupId: String) -> Unit
) {
    val groupInput = EditText(activity).apply {
        hint = "Enter or paste Group key"
    }

    val dialog = AlertDialog.Builder(activity)
        .setTitle("Join Other Group               ")
        .setMessage("Please enter the Group key to join:")
        .setView(groupInput)
        .setCancelable(true)
        .setPositiveButton("OK", null)
        .setNegativeButton("Cancel", null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val groupId = groupInput.text.toString().trim()
            if (groupId.isEmpty()) {
                Toast.makeText(activity, "Group ID cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                val allowedChars = ('A'..'Z')
                if (groupId.length == 10 && groupId.all { it in allowedChars }) {
                    dialog.dismiss()
                    onComplete(groupId)
                } else {
                    Toast.makeText(activity, "Invalid group key. Must be 10 uppercase letters.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    dialog.show()
}

/**
 * HELPER: Generates a random ID for creating a new group.
 */
private fun generateRandomGroupId(length: Int = 10): String {
    val allowedChars = ('A'..'Z')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

