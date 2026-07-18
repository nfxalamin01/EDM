package com.edm.downloadmanager.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edm.downloadmanager.data.db.DownloadEntity
import com.edm.downloadmanager.viewmodel.DownloadViewModel
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

private fun shareFile(context: android.content.Context, download: DownloadEntity) {
    val file = File(download.filePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = download.mimeType ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share ${download.fileName}"))
}

@Composable
fun DownloadsScreen(viewModel: DownloadViewModel = viewModel(), onOpenDownloadDetail: (Long) -> Unit) {
    val context = LocalContext.current
    val all by viewModel.allDownloads.collectAsState()
    DownloadListScreen("All Downloads", all, viewModel, onOpenDownloadDetail) { shareFile(context, it) }
}

@Composable
fun QueueScreen(viewModel: DownloadViewModel = viewModel(), onOpenDownloadDetail: (Long) -> Unit) {
    val context = LocalContext.current
    val queued by viewModel.queuedDownloads.collectAsState()
    DownloadListScreen("Queue", queued, viewModel, onOpenDownloadDetail) { shareFile(context, it) }
}

@Composable
fun HistoryScreen(viewModel: DownloadViewModel = viewModel(), onOpenDownloadDetail: (Long) -> Unit) {
    val context = LocalContext.current
    val completed by viewModel.completedDownloads.collectAsState()
    DownloadListScreen("History", completed, viewModel, onOpenDownloadDetail) { shareFile(context, it) }
}

@Composable
fun FavoritesScreen(viewModel: DownloadViewModel = viewModel(), onOpenDownloadDetail: (Long) -> Unit) {
    val context = LocalContext.current
    val favorites by viewModel.favoriteDownloads.collectAsState()
    DownloadListScreen("Favorites", favorites, viewModel, onOpenDownloadDetail) { shareFile(context, it) }
}
