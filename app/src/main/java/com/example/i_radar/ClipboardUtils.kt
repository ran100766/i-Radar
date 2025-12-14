package com.example.i_radar

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Group Key", text))
    Toast.makeText(context, "Group key copied: " + text, Toast.LENGTH_SHORT).show()
}
