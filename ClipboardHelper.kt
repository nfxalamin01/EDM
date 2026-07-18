package com.edm.downloadmanager.util

import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {

    private val urlRegex = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

    /** Returns a direct download-looking URL currently on the clipboard, or null. */
    fun detectDownloadLink(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        if (!clipboard.hasPrimaryClip()) return null
        val item = clipboard.primaryClip?.getItemAt(0) ?: return null
        val text = item.text?.toString()?.trim() ?: return null
        return if (urlRegex.matches(text)) text else null
    }
}
