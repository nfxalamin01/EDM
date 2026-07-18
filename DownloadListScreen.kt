package com.edm.downloadmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edm.downloadmanager.data.db.DownloadEntity
import com.edm.downloadmanager.ui.components.DownloadItemCard
import com.edm.downloadmanager.viewmodel.DownloadViewModel

enum class SortOption { DATE_NEWEST, NAME_AZ, SIZE_LARGEST }

/**
 * Shared list screen implementation backing Downloads, Queue, History and
 * Favorites tabs. Provides search, filter-by-category and sort so each
 * screen doesn't reimplement the same list-management UI.
 */
@Composable
fun DownloadListScreen(
    title: String,
    downloads: List<DownloadEntity>,
    viewModel: DownloadViewModel,
    onOpenDownloadDetail: (Long) -> Unit,
    onShare: (DownloadEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.DATE_NEWEST) }
    var categoryFilter by remember { mutableStateOf<com.edm.downloadmanager.data.db.FileCategory?>(null) }

    val filtered = remember(downloads, query, sortOption, categoryFilter) {
        downloads
            .filter { query.isBlank() || it.fileName.contains(query, ignoreCase = true) }
            .filter { categoryFilter == null || it.fileCategory == categoryFilter }
            .sortedWith(
                when (sortOption) {
                    SortOption.DATE_NEWEST -> compareByDescending { it.createdAt }
                    SortOption.NAME_AZ -> compareBy { it.fileName.lowercase() }
                    SortOption.SIZE_LARGEST -> compareByDescending { it.totalBytes }
                }
            )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search downloads") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var sortMenu by remember { mutableStateOf(false) }
            Box {
                AssistChip(onClick = { sortMenu = true }, label = { Text("Sort") }, leadingIcon = { Icon(Icons.Filled.SortByAlpha, null) })
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    DropdownMenuItem(text = { Text("Newest first") }, onClick = { sortOption = SortOption.DATE_NEWEST; sortMenu = false })
                    DropdownMenuItem(text = { Text("Name A-Z") }, onClick = { sortOption = SortOption.NAME_AZ; sortMenu = false })
                    DropdownMenuItem(text = { Text("Largest first") }, onClick = { sortOption = SortOption.SIZE_LARGEST; sortMenu = false })
                }
            }
            var filterMenu by remember { mutableStateOf(false) }
            Box {
                AssistChip(onClick = { filterMenu = true }, label = { Text(categoryFilter?.name ?: "Filter") }, leadingIcon = { Icon(Icons.Filled.FilterList, null) })
                DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                    DropdownMenuItem(text = { Text("All") }, onClick = { categoryFilter = null; filterMenu = false })
                    com.edm.downloadmanager.data.db.FileCategory.values().forEach { cat ->
                        DropdownMenuItem(text = { Text(cat.name) }, onClick = { categoryFilter = cat; filterMenu = false })
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Text("Nothing here yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { d ->
                    DownloadItemCard(
                        download = d,
                        onPauseResume = { if (d.status.name == "DOWNLOADING") viewModel.pause(d) else viewModel.resume(d) },
                        onRestart = { viewModel.restart(d) },
                        onStop = { viewModel.stop(d) },
                        onFavoriteToggle = { viewModel.toggleFavorite(d) },
                        onDelete = { viewModel.deleteDownload(d, alsoDeleteFile = true) },
                        onShare = { onShare(d) },
                        onClick = { onOpenDownloadDetail(d.id) }
                    )
                }
            }
        }
    }
}
