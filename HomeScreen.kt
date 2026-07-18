package com.edm.downloadmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edm.downloadmanager.ui.components.DownloadItemCard
import com.edm.downloadmanager.ui.components.StatCard
import com.edm.downloadmanager.util.ClipboardHelper
import com.edm.downloadmanager.util.FileUtils
import com.edm.downloadmanager.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DownloadViewModel = viewModel(),
    onOpenQrScanner: () -> Unit,
    onOpenDownloadDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    val active by viewModel.activeDownloads.collectAsState()
    val todayCount by viewModel.todayCount.collectAsState()
    val todayBytes by viewModel.todayBytes.collectAsState()
    val weekBytes by viewModel.weekBytes.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("EDM Dashboard", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Today's Downloads", "$todayCount", Modifier.weight(1f))
            StatCard("Today's Data", FileUtils.formatBytes(todayBytes), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        StatCard("This Week", FileUtils.formatBytes(weekBytes), Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Paste a direct download link") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row {
                    IconButton(onClick = {
                        ClipboardHelper.detectDownloadLink(context)?.let { urlInput = it }
                    }) { Icon(Icons.Filled.ContentPaste, contentDescription = "Paste from clipboard") }
                    IconButton(onClick = onOpenQrScanner) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan QR code")
                    }
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (urlInput.isNotBlank()) {
                    viewModel.addDownloadFromUrl(urlInput.trim())
                    urlInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start Download") }

        Spacer(Modifier.height(20.dp))
        Text("Active Downloads", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (active.isEmpty()) {
            Text("No active downloads. Paste a link above to get started.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(active, key = { it.id }) { d ->
                    DownloadItemCard(
                        download = d,
                        onPauseResume = { if (d.status.name == "DOWNLOADING") viewModel.pause(d) else viewModel.resume(d) },
                        onRestart = { viewModel.restart(d) },
                        onStop = { viewModel.stop(d) },
                        onFavoriteToggle = { viewModel.toggleFavorite(d) },
                        onDelete = { viewModel.deleteDownload(d, alsoDeleteFile = true) },
                        onShare = { /* handled via FileProvider intent in FileManagerScreen */ },
                        onClick = { onOpenDownloadDetail(d.id) }
                    )
                }
            }
        }
    }
}
