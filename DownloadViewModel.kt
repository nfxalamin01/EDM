package com.edm.downloadmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edm.downloadmanager.EDMApplication
import com.edm.downloadmanager.data.db.DownloadEntity
import com.edm.downloadmanager.data.db.DownloadStatus
import com.edm.downloadmanager.engine.RangeSupportChecker
import com.edm.downloadmanager.service.DownloadService
import com.edm.downloadmanager.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewModel(app: Application) : AndroidViewModel(app) {

    private val edmApp = app as EDMApplication
    private val repo = edmApp.downloadRepository
    private val settings = edmApp.settingsStore

    val allDownloads = repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeDownloads = repo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val queuedDownloads = repo.observeQueue().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val completedDownloads = repo.observeCompleted().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val failedDownloads = repo.observeFailed().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteDownloads = repo.observeFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCount = repo.observeTodayCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val todayBytes = repo.observeTodayBytes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val weekBytes = repo.observeWeekBytes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val monthBytes = repo.observeMonthBytes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _newDownloadState = MutableStateFlow<NewDownloadState>(NewDownloadState.Idle)
    val newDownloadState: StateFlow<NewDownloadState> = _newDownloadState

    sealed class NewDownloadState {
        object Idle : NewDownloadState()
        object Analyzing : NewDownloadState()
        data class Error(val message: String) : NewDownloadState()
    }

    fun searchDownloads(query: String) = repo.search(query)

    /** Analyzes the URL (built-in URL analyzer) then enqueues via the foreground service. */
    fun addDownloadFromUrl(url: String, priority: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repo.findDuplicate(url)
                if (existing != null) {
                    // Duplicate file detection: surface as a failed/idle notice; UI can prompt to re-download.
                    return@launch
                }
                val analysis = RangeSupportChecker.analyze(url)
                val category = com.edm.downloadmanager.util.FileUtils.categorize(analysis.suggestedFileName)
                val sanitized = FileUtils.sanitizeFileName(analysis.suggestedFileName)
                val subfolder = FileUtils.subfolderFor(category)
                val baseDir = File(edmApp.getExternalFilesDir(null), "EDM/$subfolder")
                val destFile = FileUtils.resolveNonConflictingFile(baseDir, sanitized)

                val threadCount = if (analysis.supportsRange && analysis.contentLength > 5_000_000) settings.threadCount else 1

                val entity = DownloadEntity(
                    url = analysis.finalUrl,
                    fileName = destFile.name,
                    filePath = destFile.absolutePath,
                    fileCategory = category,
                    totalBytes = analysis.contentLength,
                    status = DownloadStatus.QUEUED,
                    supportsRangeRequests = analysis.supportsRange,
                    threadCount = threadCount,
                    priority = priority,
                    mimeType = analysis.mimeType
                )
                val id = repo.addDownload(entity)
                DownloadService.enqueue(edmApp, id)
                _newDownloadState.value = NewDownloadState.Idle
            } catch (e: Exception) {
                _newDownloadState.value = NewDownloadState.Error(e.message ?: "Failed to analyze URL")
            }
        }
    }

    fun pause(download: DownloadEntity) {
        DownloadService.pause(edmApp, download.id)
    }

    fun resume(download: DownloadEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateStatus(download, DownloadStatus.QUEUED)
            DownloadService.enqueue(edmApp, download.id)
        }
    }

    fun restart(download: DownloadEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            File(download.filePath).delete()
            repo.updateFull(download.copy(downloadedBytes = 0, status = DownloadStatus.QUEUED, retryCount = 0))
            DownloadService.enqueue(edmApp, download.id)
        }
    }

    fun stop(download: DownloadEntity) {
        DownloadService.cancel(edmApp, download.id)
    }

    fun deleteDownload(download: DownloadEntity, alsoDeleteFile: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (alsoDeleteFile) File(download.filePath).delete()
            repo.delete(download)
        }
    }

    fun rename(download: DownloadEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) { repo.rename(download, newName) }
    }

    fun toggleFavorite(download: DownloadEntity) {
        viewModelScope.launch(Dispatchers.IO) { repo.toggleFavorite(download) }
    }

    fun changePriority(download: DownloadEntity, newPriority: Int) {
        viewModelScope.launch(Dispatchers.IO) { repo.updateFull(download.copy(priority = newPriority)) }
    }
}
