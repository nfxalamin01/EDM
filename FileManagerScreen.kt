package com.edm.downloadmanager.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.edm.downloadmanager.util.FileUtils
import java.io.File

/**
 * Browses the app's organized download folders (EDM/Videos, EDM/Documents, ...)
 * directly from the filesystem so the user can inspect files even ones added
 * outside the app's database (e.g. moved manually), and open/share them.
 */
@Composable
fun FileManagerScreen() {
    val context = LocalContext.current
    val rootDir = remember { File(context.getExternalFilesDir(null), "EDM") }
    var currentDir by remember { mutableStateOf(rootDir) }

    val entries = remember(currentDir) {
        currentDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("File Manager", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(currentDir.absolutePath.substringAfter("EDM"), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))

        if (currentDir != rootDir) {
            TextButton(onClick = { currentDir = currentDir.parentFile ?: rootDir }) { Text("← Back") }
        }

        if (entries.isEmpty()) {
            Text("No files here yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(entries) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.name) },
                        supportingContent = { if (!entry.isDirectory) Text(FileUtils.formatBytes(entry.length())) },
                        leadingContent = {
                            Icon(if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            if (entry.isDirectory) {
                                currentDir = entry
                            } else {
                                openFile(context, entry)
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

private fun openFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open with"))
}

