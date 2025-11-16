// File: DialogUtils.kt
package com.example.i_radar

import android.app.Activity
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

fun askUserNameAndGroup(
    activity: Activity,
    onDataEntered: (name: String, groupId: String) -> Unit
) {
    // Step 1: Ask for name
    val nameEditText = EditText(activity).apply { hint = "Enter your name" }

    val nameDialog = AlertDialog.Builder(activity)
        .setTitle("Welcome")
        .setMessage("Please enter your name:")
        .setView(nameEditText)
        .setCancelable(false)
        .setPositiveButton("OK", null)
        .create()

    nameDialog.setOnShowListener {
        val okButton = nameDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        okButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(activity, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                nameDialog.dismiss()

                // Step 2: Ask for Group ID
                val groupEditText = EditText(activity).apply { hint = "Enter group ID" }

                val groupDialog = AlertDialog.Builder(activity)
                    .setTitle("Group")
                    .setMessage("Please enter your group ID:")
                    .setView(groupEditText)
                    .setCancelable(false)
                    .setPositiveButton("OK", null)
                    .create()

                groupDialog.setOnShowListener {
                    val groupOkButton = groupDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    groupOkButton.setOnClickListener {
                        val groupId = groupEditText.text.toString().trim()
                        if (groupId.isEmpty()) {
                            Toast.makeText(activity, "Group ID cannot be empty", Toast.LENGTH_SHORT).show()
                        } else {
                            groupDialog.dismiss()
                            onDataEntered(name, groupId)
                            Toast.makeText(activity, "Hello, $name! Group: $groupId", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                groupDialog.show()
            }
        }
    }

    nameDialog.show()
}
