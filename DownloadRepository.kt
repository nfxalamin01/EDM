package com.edm.downloadmanager.data.repository

import com.edm.downloadmanager.data.db.DownloadDao
import com.edm.downloadmanager.data.db.DownloadEntity
import com.edm.downloadmanager.data.db.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for download data used by all UI screens
 * (Home, Downloads, Queue, History, Favorites, Statistics).
 * The actual byte transfer happens in engine.DownloadEngine / service.DownloadService;
 * this class only persists/reads state.
 */
class DownloadRepository(private val dao: DownloadDao) {

    fun observeAll(): Flow<List<DownloadEntity>> = dao.observeAll()
    fun observeActive(): Flow<List<DownloadEntity>> = dao.observeActive()
    fun observePaused(): Flow<List<DownloadEntity>> = dao.observePaused()
    fun observeCompleted(): Flow<List<DownloadEntity>> = dao.observeCompleted()
    fun observeFailed(): Flow<List<DownloadEntity>> = dao.observeFailed()
    fun observeFavorites(): Flow<List<DownloadEntity>> = dao.observeFavorites()
    fun observeQueue(): Flow<List<DownloadEntity>> = dao.observeQueue()
    fun observeById(id: Long): Flow<DownloadEntity?> = dao.observeById(id)
    fun search(query: String): Flow<List<DownloadEntity>> = dao.search(query)

    fun observeTodayCount(): Flow<Int> = dao.observeCountSince(startOfTodayMillis())
    fun observeTodayBytes(): Flow<Long> = dao.observeBytesSince(startOfTodayMillis())
    fun observeWeekBytes(): Flow<Long> = dao.observeBytesSince(System.currentTimeMillis() - 7L * 86_400_000)
    fun observeMonthBytes(): Flow<Long> = dao.observeBytesSince(System.currentTimeMillis() - 30L * 86_400_000)

    suspend fun findDuplicate(url: String): DownloadEntity? = dao.findDuplicateByUrl(url)

    suspend fun addDownload(entity: DownloadEntity): Long = dao.insert(entity)

    suspend fun updateStatus(download: DownloadEntity, status: DownloadStatus, error: String? = null) {
        dao.update(
            download.copy(
                status = status,
                errorMessage = error,
                completedAt = if (status == DownloadStatus.COMPLETED) System.currentTimeMillis() else download.completedAt
            )
        )
    }

    suspend fun updateProgress(download: DownloadEntity, downloadedBytes: Long, speedBps: Long) {
        dao.update(download.copy(downloadedBytes = downloadedBytes, speedBytesPerSec = speedBps))
    }

    suspend fun toggleFavorite(download: DownloadEntity) {
        dao.update(download.copy(isFavorite = !download.isFavorite))
    }

    suspend fun rename(download: DownloadEntity, newName: String) {
        dao.update(download.copy(fileName = newName))
    }

    suspend fun delete(download: DownloadEntity) {
        dao.deleteSegments(download.id)
        dao.delete(download)
    }

    suspend fun updateFull(download: DownloadEntity) = dao.update(download)

    private fun startOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
