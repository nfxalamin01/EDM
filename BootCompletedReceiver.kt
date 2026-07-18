package com.edm.downloadmanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.edm.downloadmanager.EDMApplication
import com.edm.downloadmanager.data.db.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Resumes interrupted downloads after device reboot, if "Auto Resume" is enabled in Settings. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as EDMApplication
        if (!app.settingsStore.autoResumeEnabled) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = app.database.downloadDao()
            val stuck = dao.observeActive()
            // observeActive is a Flow; for a one-shot boot resume we just query queued+downloading directly.
            stuck.collect { list ->
                list.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
                    .forEach { DownloadService.enqueue(context, it.id) }
                return@collect
            }
        }
    }
}
