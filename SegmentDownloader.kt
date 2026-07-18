package com.edm.downloadmanager.engine

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Downloads a single byte-range segment of a file into the correct offset of
 * the destination file using a shared RandomAccessFile handle. Supports
 * cooperative pause/cancel via [SegmentControl] and an optional download-speed
 * limiter (bytes/sec) applied per segment.
 */
class SegmentControl {
    val isPaused = AtomicBoolean(false)
    val isCancelled = AtomicBoolean(false)
}

class SegmentDownloader(
    private val client: OkHttpClient,
    private val url: String
) {
    /**
     * @param startByte inclusive start offset in the destination file
     * @param resumeFromByte byte offset already downloaded for this segment (>= startByte)
     * @param endByte inclusive end offset (or -1 for "to end of file", used for single-thread mode)
     * @param onBytesRead invoked after every chunk with the number of new bytes written
     * @param maxBytesPerSecOrNull null = unlimited
     */
    fun downloadSegment(
        destFile: RandomAccessFile,
        startByte: Long,
        resumeFromByte: Long,
        endByte: Long,
        control: SegmentControl,
        maxBytesPerSecOrNull: Long? = null,
        onBytesRead: (Long) -> Unit
    ) {
        val rangeHeader = if (endByte >= 0) "bytes=$resumeFromByte-$endByte" else "bytes=$resumeFromByte-"
        val request = Request.Builder()
            .url(url)
            .header("Range", rangeHeader)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Segment request failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw java.io.IOException("Empty response body")
            body.byteStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var writeOffset = resumeFromByte
                var bytesThisWindow = 0L
                var windowStart = System.currentTimeMillis()

                while (true) {
                    if (control.isCancelled.get()) return
                    while (control.isPaused.get() && !control.isCancelled.get()) {
                        Thread.sleep(200)
                    }
                    if (control.isCancelled.get()) return

                    val read = input.read(buffer)
                    if (read == -1) break

                    synchronized(destFile) {
                        destFile.seek(writeOffset)
                        destFile.write(buffer, 0, read)
                    }
                    writeOffset += read
                    onBytesRead(read.toLong())

                    // Simple speed limiter: sleep if we've exceeded the allotted
                    // bytes for the current 1-second window.
                    maxBytesPerSecOrNull?.let { limit ->
                        bytesThisWindow += read
                        val elapsed = System.currentTimeMillis() - windowStart
                        if (bytesThisWindow >= limit) {
                            val remainingMs = 1000 - elapsed
                            if (remainingMs > 0) Thread.sleep(remainingMs)
                            bytesThisWindow = 0
                            windowStart = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }
}
