package com.edm.downloadmanager.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.edm.downloadmanager.EDMApplication
import com.edm.downloadmanager.data.db.DownloadStatus
import com.edm.downloadmanager.engine.DownloadEngine
import com.edm.downloadmanager.engine.EngineResult
import com.edm.downloadmanager.util.ChecksumUtil
import com.edm.downloadmanager.util.FileUtils
import com.edm.downloadmanager.util.NetworkUtils
import com.edm.downloadmanager.util.NotificationHelper
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

/**
 * Foreground service that owns the actual download engine execution so
 * downloads keep running in the background, survive Activity destruction,
 * and are exempt from Doze-related throttling while a notification is shown.
 *
 * Respects "Max simultaneous downloads", "Wi-Fi only mode", "Mobile data
 * controls" and "Speed limiter" settings from SettingsStore.
 */
class DownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engine = DownloadEngine()
    private lateinit var app: EDMApplication
    private val runningJobs = ConcurrentHashMap<Long, Job>()
    private lateinit var concurrencyGate: Semaphore

    override fun onCreate() {
        super.onCreate()
        app = application as EDMApplication
        NotificationHelper.ensureChannels(this)
        concurrencyGate = Semaphore(app.settingsStore.maxSimultaneousDownloads)
        startForeground(
            NotificationHelper.FOREGROUND_NOTIFICATION_ID,
            NotificationHelper.buildProgressNotification(this, "EDM is running", 0, "Idle", "--:--").build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENQUEUE -> intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1).takeIf { it >= 0 }?.let { enqueue(it) }
            ACTION_PAUSE -> intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1).takeIf { it >= 0 }?.let { engine.pause(it) }
            ACTION_CANCEL -> intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1).takeIf { it >= 0 }?.let {
                engine.cancel(it)
                runningJobs[it]?.cancel()
            }
        }
        return START_STICKY
    }

    private fun enqueue(downloadId: Long) {
        val job = serviceScope.launch {
            concurrencyGate.acquire()
            try {
                runDownload(downloadId)
            } finally {
                concurrencyGate.release()
                runningJobs.remove(downloadId)
                if (runningJobs.isEmpty()) stopSelf()
            }
        }
        runningJobs[downloadId] = job
    }

    private suspend fun runDownload(downloadId: Long) {
        val repo = app.downloadRepository
        val dao = app.database.downloadDao()
        var download = dao.getById(downloadId) ?: return

        if (!NetworkUtils.isAllowedToDownload(this, app.settingsStore.wifiOnlyMode)) {
            repo.updateStatus(download, DownloadStatus.FAILED, "No allowed network connection (Wi-Fi only mode)")
            return
        }

        repo.updateStatus(download, DownloadStatus.DOWNLOADING)
        val existingSegments = dao.getSegments(downloadId)
        val speedLimit = app.settingsStore.speedLimitBytesPerSec.takeIf { it > 0 }

        val result = engine.run(
            downloadId = downloadId,
            url = download.url,
            destPath = download.filePath,
            totalBytes = download.totalBytes,
            supportsRange = download.supportsRangeRequests,
            threadCount = download.threadCount,
            existingSegments = existingSegments,
            maxTotalBytesPerSec = speedLimit,
            onProgress = { progress ->
                serviceScope.launch {
                    repo.updateProgress(dao.getById(downloadId) ?: return@launch, progress.downloadedBytes, progress.speedBytesPerSec)
                    updateNotification(download.fileName, progress.downloadedBytes, progress.totalBytes, progress.speedBytesPerSec)
                }
            },
            onSegmentUpdate = { segment ->
                serviceScope.launch { dao.updateSegment(segment) }
            }
        )

        download = dao.getById(downloadId) ?: return
        when (result) {
            is EngineResult.Success -> {
                val file = File(download.filePath)
                val verified = ChecksumUtil.verify(file, download.checksumExpected)
                repo.updateFull(
                    download.copy(
                        status = DownloadStatus.COMPLETED,
                        completedAt = System.currentTimeMillis(),
                        checksumVerified = verified,
                        downloadedBytes = file.length()
                    )
                )
                showCompleteNotification(download.fileName)
            }
            is EngineResult.Failure -> {
                if (result.error == "Paused") {
                    repo.updateStatus(download, DownloadStatus.PAUSED)
                } else if (download.retryCount < 3 && result.retryable) {
                    repo.updateFull(download.copy(retryCount = download.retryCount + 1))
                    enqueue(downloadId) // smart retry
                } else {
                    repo.updateStatus(download, DownloadStatus.FAILED, result.error)
                }
            }
            is EngineResult.Cancelled -> {
                repo.updateStatus(download, DownloadStatus.CANCELLED)
            }
        }
    }

    private fun updateNotification(name: String, downloaded: Long, total: Long, speed: Long) {
        val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
        val eta = FileUtils.formatEta(total - downloaded, speed)
        val notification = NotificationHelper.buildProgressNotification(
            this, name, percent, FileUtils.formatSpeed(speed), eta
        ).build()
        NotificationManagerCompat.from(this).notify(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun showCompleteNotification(name: String) {
        val notification = NotificationHelper.buildCompleteNotification(this, name).build()
        NotificationManagerCompat.from(this).notify(name.hashCode(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_ENQUEUE = "com.edm.downloadmanager.ACTION_ENQUEUE"
        const val ACTION_PAUSE = "com.edm.downloadmanager.ACTION_PAUSE"
        const val ACTION_CANCEL = "com.edm.downloadmanager.ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun enqueue(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java)
                .setAction(ACTION_ENQUEUE)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context, downloadId: Long) {
            context.startService(
                Intent(context, DownloadService::class.java).setAction(ACTION_PAUSE).putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            )
        }

        fun cancel(context: Context, downloadId: Long) {
            context.startService(
                Intent(context, DownloadService::class.java).setAction(ACTION_CANCEL).putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            )
        }
    }
}
