package com.edm.downloadmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edm.downloadmanager.data.db.DownloadEntity
import com.edm.downloadmanager.data.db.DownloadStatus
import com.edm.downloadmanager.util.FileUtils

/**
 * A single row shown on Home, Downloads, Queue, History and Favorites screens.
 * Exposes pause/resume/restart/stop/favorite/delete/share actions via callbacks
 * so each screen can decide which actions make sense for that list.
 */
@Composable
fun DownloadItemCard(
    download: DownloadEntity,
    onPauseResume: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(categoryIcon(download), contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(download.fileName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        statusLabel(download),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        if (download.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Favorite"
                    )
                }
            }

            if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PAUSED) {
                Spacer(Modifier.height(8.dp))
                val progress = if (download.totalBytes > 0) download.downloadedBytes / download.totalBytes.toFloat() else 0f
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    "${FileUtils.formatBytes(download.downloadedBytes)} / ${FileUtils.formatBytes(download.totalBytes)} · ${FileUtils.formatSpeed(download.speedBytesPerSec)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(6.dp))
            Row {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> ActionIcon(Icons.Filled.Pause, "Pause", onPauseResume)
                    DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.QUEUED -> ActionIcon(Icons.Filled.PlayArrow, "Resume", onPauseResume)
                    else -> {}
                }
                ActionIcon(Icons.Filled.Refresh, "Restart", onRestart)
                if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.QUEUED) {
                    ActionIcon(Icons.Filled.Stop, "Stop", onStop)
                }
                if (download.status == DownloadStatus.COMPLETED) {
                    ActionIcon(Icons.Filled.Share, "Share", onShare)
                }
                Spacer(Modifier.weight(1f))
                ActionIcon(Icons.Filled.Delete, "Delete", onDelete)
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
}

private fun statusLabel(download: DownloadEntity): String = when (download.status) {
    DownloadStatus.QUEUED -> "Queued"
    DownloadStatus.DOWNLOADING -> "Downloading"
    DownloadStatus.PAUSED -> "Paused"
    DownloadStatus.COMPLETED -> "Completed · ${FileUtils.formatBytes(download.totalBytes)}"
    DownloadStatus.FAILED -> "Failed: ${download.errorMessage ?: "Unknown error"}"
    DownloadStatus.CANCELLED -> "Cancelled"
}

private fun categoryIcon(download: DownloadEntity) = when (download.fileCategory) {
    com.edm.downloadmanager.data.db.FileCategory.VIDEO -> Icons.Filled.Movie
    com.edm.downloadmanager.data.db.FileCategory.AUDIO -> Icons.Filled.MusicNote
    com.edm.downloadmanager.data.db.FileCategory.IMAGE -> Icons.Filled.Image
    com.edm.downloadmanager.data.db.FileCategory.DOCUMENT -> Icons.Filled.Description
    com.edm.downloadmanager.data.db.FileCategory.APK -> Icons.Filled.Android
    com.edm.downloadmanager.data.db.FileCategory.ARCHIVE -> Icons.Filled.FolderZip
    com.edm.downloadmanager.data.db.FileCategory.ISO -> Icons.Filled.Album
    com.edm.downloadmanager.data.db.FileCategory.OTHER -> Icons.Filled.InsertDriveFile
}
