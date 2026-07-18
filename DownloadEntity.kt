package com.edm.downloadmanager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

enum class FileCategory {
    VIDEO, AUDIO, IMAGE, DOCUMENT, APK, ARCHIVE, ISO, OTHER
}

/**
 * A single download task persisted to Room. Segment progress for multi-thread
 * downloads is tracked separately in [DownloadSegmentEntity] so a download can
 * be paused/resumed segment-by-segment.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val fileCategory: FileCategory = FileCategory.OTHER,
    val totalBytes: Long = -1L,          // -1 until server responds with Content-Length
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val supportsRangeRequests: Boolean = false,
    val threadCount: Int = 1,
    val priority: Int = 0,               // higher = downloaded first
    val speedBytesPerSec: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val isFavorite: Boolean = false,
    val checksumExpected: String? = null,
    val checksumActual: String? = null,
    val checksumVerified: Boolean = false,
    val scheduledTimeMillis: Long? = null,
    val mimeType: String? = null
)

/** One HTTP-Range segment of a multi-thread download. */
@Entity(tableName = "download_segments", primaryKeys = ["downloadId", "segmentIndex"])
data class DownloadSegmentEntity(
    val downloadId: Long,
    val segmentIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long = 0L,
    val isComplete: Boolean = false
)
