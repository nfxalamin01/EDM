package com.edm.downloadmanager.engine

import com.edm.downloadmanager.data.db.DownloadSegmentEntity
import okhttp3.OkHttpClient
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class EngineProgress(
    val downloadId: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long
)

sealed class EngineResult {
    data class Success(val downloadId: Long) : EngineResult()
    data class Failure(val downloadId: Long, val error: String, val retryable: Boolean) : EngineResult()
    data class Cancelled(val downloadId: Long) : EngineResult()
}

/**
 * Core high-speed download engine.
 *
 * - If the server supports HTTP Range requests and file size is known, splits
 *   the file into N segments and downloads them concurrently (segmented
 *   multi-thread download), each writing directly to its byte offset in the
 *   pre-allocated destination file.
 * - Falls back to a single-stream download otherwise.
 * - Exposes pause/resume/cancel per download via [SegmentControl] registry.
 * - Applies smart retry with exponential backoff for transient network errors.
 * - Applies an optional global speed limit, split evenly across active segments.
 */
class DownloadEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val controls = ConcurrentHashMap<Long, SegmentControl>()
    private val executor = Executors.newCachedThreadPool()

    fun pause(downloadId: Long) {
        controls[downloadId]?.isPaused?.set(true)
    }

    fun resumeFlag(downloadId: Long) {
        controls[downloadId]?.isPaused?.set(false)
    }

    fun cancel(downloadId: Long) {
        controls[downloadId]?.isCancelled?.set(true)
    }

    fun analyze(url: String): UrlAnalysis = RangeSupportChecker.analyze(url)

    /**
     * Runs the download synchronously on the calling thread (call from a
     * background/foreground-service worker thread, never the UI thread).
     * Progress + segment persistence callbacks let the caller update Room
     * and the notification without the engine depending on those layers.
     */
    fun run(
        downloadId: Long,
        url: String,
        destPath: String,
        totalBytes: Long,
        supportsRange: Boolean,
        threadCount: Int,
        existingSegments: List<DownloadSegmentEntity>,
        maxTotalBytesPerSec: Long?,
        onProgress: (EngineProgress) -> Unit,
        onSegmentUpdate: (DownloadSegmentEntity) -> Unit
    ): EngineResult {
        val control = SegmentControl()
        controls[downloadId] = control

        val destFile = File(destPath)
        destFile.parentFile?.mkdirs()

        val downloadedTotal = AtomicLong(existingSegments.sumOf { it.downloadedBytes }.takeIf { existingSegments.isNotEmpty() } ?: 0L)
        var lastSpeedSampleTime = System.currentTimeMillis()
        var lastSpeedSampleBytes = downloadedTotal.get()

        val perSegmentLimit = maxTotalBytesPerSec?.let { it / maxOf(1, threadCount) }

        return try {
            RandomAccessFile(destFile, "rw").use { raf ->
                if (totalBytes > 0) raf.setLength(totalBytes)

                val segments = if (existingSegments.isNotEmpty()) {
                    existingSegments
                } else if (supportsRange && totalBytes > 0 && threadCount > 1) {
                    buildSegments(downloadId, totalBytes, threadCount)
                } else {
                    listOf(DownloadSegmentEntity(downloadId, 0, 0, totalBytes - 1))
                }

                val downloader = SegmentDownloader(client, url)
                val futures = segments.filterNot { it.isComplete }.map { segment ->
                    executor.submit {
                        var attempt = 0
                        var lastError: Exception? = null
                        var segState = segment
                        while (attempt < MAX_RETRIES && !control.isCancelled.get()) {
                            try {
                                downloader.downloadSegment(
                                    destFile = raf,
                                    startByte = segState.startByte,
                                    resumeFromByte = segState.startByte + segState.downloadedBytes,
                                    endByte = segState.endByte,
                                    control = control,
                                    maxBytesPerSecOrNull = perSegmentLimit
                                ) { newBytes ->
                                    segState = segState.copy(downloadedBytes = segState.downloadedBytes + newBytes)
                                    val total = downloadedTotal.addAndGet(newBytes)
                                    onSegmentUpdate(segState)

                                    val now = System.currentTimeMillis()
                                    if (now - lastSpeedSampleTime >= 500) {
                                        val speed = ((total - lastSpeedSampleBytes) * 1000) / (now - lastSpeedSampleTime)
                                        onProgress(EngineProgress(downloadId, total, totalBytes, speed))
                                        lastSpeedSampleTime = now
                                        lastSpeedSampleBytes = total
                                    }
                                }
                                segState = segState.copy(isComplete = true)
                                onSegmentUpdate(segState)
                                lastError = null
                                break
                            } catch (e: Exception) {
                                lastError = e
                                if (control.isPaused.get() || control.isCancelled.get()) break
                                attempt++
                                Thread.sleep(RETRY_BACKOFF_MS * attempt) // smart retry / auto-reconnect
                            }
                        }
                        lastError
                    }
                }

                val errors = futures.mapNotNull { it.get() }

                when {
                    control.isCancelled.get() -> EngineResult.Cancelled(downloadId)
                    control.isPaused.get() -> EngineResult.Failure(downloadId, "Paused", retryable = true)
                    errors.isNotEmpty() -> EngineResult.Failure(downloadId, errors.first().message ?: "Unknown error", retryable = true)
                    else -> EngineResult.Success(downloadId)
                }
            }
        } catch (e: Exception) {
            EngineResult.Failure(downloadId, e.message ?: "Unknown error", retryable = true)
        } finally {
            controls.remove(downloadId)
        }
    }

    private fun buildSegments(downloadId: Long, totalBytes: Long, threadCount: Int): List<DownloadSegmentEntity> {
        val chunkSize = totalBytes / threadCount
        return (0 until threadCount).map { i ->
            val start = i * chunkSize
            val end = if (i == threadCount - 1) totalBytes - 1 else start + chunkSize - 1
            DownloadSegmentEntity(downloadId, i, start, end)
        }
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val RETRY_BACKOFF_MS = 1500L
    }
}
