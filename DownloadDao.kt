package com.edm.downloadmanager.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeById(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads ORDER BY priority DESC, createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING','QUEUED') ORDER BY priority DESC, createdAt ASC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'PAUSED' ORDER BY createdAt DESC")
    fun observePaused(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'FAILED' ORDER BY createdAt DESC")
    fun observeFailed(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY priority DESC, createdAt ASC")
    fun observeQueue(): Flow<List<DownloadEntity>>

    @Query("""
        SELECT * FROM downloads
        WHERE fileName LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun search(query: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE url = :url AND status != 'CANCELLED' LIMIT 1")
    suspend fun findDuplicateByUrl(url: String): DownloadEntity?

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'COMPLETED' AND completedAt >= :sinceMillis")
    fun observeCountSince(sinceMillis: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalBytes), 0) FROM downloads WHERE status = 'COMPLETED' AND completedAt >= :sinceMillis")
    fun observeBytesSince(sinceMillis: Long): Flow<Long>

    // Segments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<DownloadSegmentEntity>)

    @Update
    suspend fun updateSegment(segment: DownloadSegmentEntity)

    @Query("SELECT * FROM download_segments WHERE downloadId = :downloadId ORDER BY segmentIndex ASC")
    suspend fun getSegments(downloadId: Long): List<DownloadSegmentEntity>

    @Query("DELETE FROM download_segments WHERE downloadId = :downloadId")
    suspend fun deleteSegments(downloadId: Long)
}
