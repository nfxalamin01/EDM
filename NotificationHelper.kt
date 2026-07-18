package com.edm.downloadmanager.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_DOWNLOADS = "edm_downloads"
    const val CHANNEL_COMPLETE = "edm_complete"
    const val FOREGROUND_NOTIFICATION_ID = 4201

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DOWNLOADS, "Active Downloads", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_COMPLETE, "Completed Downloads", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progressPercent: Int,
        speedLabel: String,
        etaLabel: String
    ): androidx.core.app.NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setContentTitle(title)
            .setContentText("$speedLabel · ETA $etaLabel")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent, progressPercent <= 0)
    }

    fun buildCompleteNotification(context: Context, title: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setContentTitle("Download complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
    }
}
