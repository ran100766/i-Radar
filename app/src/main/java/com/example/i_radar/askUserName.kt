package com.example.i_radar

import android.app.Activity
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.i_radar.MainActivity.Companion.defaultGroupId


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
        val ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        ok.setOnClickListener {
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
 * PUBLIC: Asks the user whether they want to join or create a group.
 */
fun askForGroupChoice(
    activity: Activity,
    onGroupEntered: (String) -> Unit
) {
    val options = arrayOf("Join existing group", "Create new group")

    AlertDialog.Builder(activity)
        .setTitle("Group Setup")
        .setItems(options) { _, which ->
            if (which == 0) { // Join
                askForGroupIdToJoin(activity) { groupId ->
                    onGroupEntered(groupId)
                }
            } else { // Create
                val newGroupId = generateRandomGroupId()
                Toast.makeText(activity, "New Group Created!", Toast.LENGTH_LONG).show()
                onGroupEntered(newGroupId)
            }
        }
        .setCancelable(false)
        .show()
}

/**
 * HELPER: Shows a dialog to enter a group ID for joining.
 */
private fun askForGroupIdToJoin(activity: Activity, onGroupDone: (String) -> Unit) {
    val groupInput = EditText(activity).apply {
        setText(defaultGroupId)
        setSelection(defaultGroupId.length)
    }

    val dialog = AlertDialog.Builder(activity)
        .setTitle("Join Group")
        .setMessage("Please enter the Group ID to join:")
        .setView(groupInput)
        .setCancelable(false)
        .setPositiveButton("OK", null)
        .create()

    dialog.setOnShowListener {
        val ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        ok.setOnClickListener {
            var groupId = groupInput.text.toString().trim()
            if (groupId.isEmpty()) {
                groupId = defaultGroupId
            }
            dialog.dismiss()
            onGroupDone(groupId)
        }
    }
    dialog.show()
}

/**
 * HELPER: Generates a random ID for creating a new group.
 */
private fun generateRandomGroupId(length: Int = 16): String {
    val allowedChars = ('A'..'Z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}
