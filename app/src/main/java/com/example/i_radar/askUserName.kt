// File: DialogUtils.kt
package com.example.i_radar

import android.app.Activity
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

fun askUserName(activity: Activity, onNameEntered: (String) -> Unit) {
    val editText = EditText(activity)
    editText.hint = "Enter your name"

    val dialog = AlertDialog.Builder(activity)
        .setTitle("Welcome")
        .setMessage("Please enter your name:")
        .setView(editText)
        .setCancelable(false)
        .setPositiveButton("OK", null) // we'll override this later
        .create()

    dialog.setOnShowListener {
        val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        okButton.setOnClickListener {
            val name = editText.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(activity, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                onNameEntered(name)
                Toast.makeText(activity, "Hello, $name!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    dialog.show()
}
