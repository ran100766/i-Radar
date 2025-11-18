// File: DialogUtils.kt
package com.example.i_radar

import android.app.Activity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog


fun askUserNameAndGroup(
    activity: Activity,
    onDataEntered: (name: String, groupId: String) -> Unit
) {
    // ---------------------------
    // STEP 1: GROUP SELECT SCREEN
    // ---------------------------
    val options = arrayOf("Join existing group", "Create new group")

    val groupChoiceDialog = AlertDialog.Builder(activity)
        .setTitle("Group")
        .setItems(options) { dialogInterface, which ->
            if (which == 0) {
                // JOIN EXISTING GROUP
                askForGroupId(activity, onGroupDone = { groupId ->
                    askForUserName(activity) { name ->
                        onDataEntered(name, groupId)
                    }
                })
            } else {
                // CREATE NEW GROUP
                askForGroupId(activity, onGroupDone = { groupId ->
                    askForUserName(activity) { name ->
                        onDataEntered(name, groupId)
                    }
                })
            }
        }
        .setCancelable(false)
        .create()

    groupChoiceDialog.show()
}

// --------------------------------------
// Helper #1: Ask for Group ID (join/create)
// --------------------------------------
private fun askForGroupId(activity: Activity, onGroupDone: (String) -> Unit) {
    val defaultValue = "locations"

    val groupInput = EditText(activity).apply {
        setText(defaultValue)   // <-- show default in the field
        setSelection(defaultValue.length)  // cursor at the end
    }

    val dialog = AlertDialog.Builder(activity)
        .setTitle("Group ID")
        .setMessage("Please enter group ID:")
        .setView(groupInput)
        .setCancelable(false)
        .setPositiveButton("OK", null)
        .create()

    dialog.setOnShowListener {
        val ok = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        ok.setOnClickListener {
            var groupId = groupInput.text.toString().trim()

            // If user cleared the field → use default value
            if (groupId.isEmpty()) {
                groupId = defaultValue
            }

            dialog.dismiss()
            onGroupDone(groupId)
        }
    }

    dialog.show()
}


// -----------------------------
// Helper #2: Ask for Name
// -----------------------------

private fun askForUserName(activity: Activity, onNameDone: (String) -> Unit) {
    val defaultName: String? = "Ran"   // <-- default value

    val nameInput = EditText(activity).apply {
        // Show default in the field
        if (defaultName != null) {
            setText(defaultName)
            setSelection(defaultName.length) // place cursor at the end
        } else {
            hint = "Enter your name"
        }
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

            // If field is empty:
            if (name.isEmpty()) {
                if (defaultName == null) {
                    // No default → user must enter something
                    Toast.makeText(activity, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    // Default exists → use it
                    name = defaultName
                }
            }

            dialog.dismiss()
            onNameDone(name)
        }
    }

    dialog.show()
}

