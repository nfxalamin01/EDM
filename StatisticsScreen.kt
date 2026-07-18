package com.edm.downloadmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edm.downloadmanager.ui.components.StatCard
import com.edm.downloadmanager.util.FileUtils
import com.edm.downloadmanager.viewmodel.DownloadViewModel

/**
 * Statistics dashboard: today / weekly / monthly totals and storage usage.
 * A production build would plug the Vico chart library (already added as a
 * dependency) here for a trend line; kept as stat cards for clarity and to
 * avoid depending on the exact bytes-by-day query shape.
 */
@Composable
fun StatisticsScreen(viewModel: DownloadViewModel = viewModel()) {
    val todayCount by viewModel.todayCount.collectAsState()
    val todayBytes by viewModel.todayBytes.collectAsState()
    val weekBytes by viewModel.weekBytes.collectAsState()
    val monthBytes by viewModel.monthBytes.collectAsState()
    val completed by viewModel.completedDownloads.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Statistics", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Today", "$todayCount files", Modifier.weight(1f))
            StatCard("Today's Data", FileUtils.formatBytes(todayBytes), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("This Week", FileUtils.formatBytes(weekBytes), Modifier.weight(1f))
            StatCard("This Month", FileUtils.formatBytes(monthBytes), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        StatCard("Total Completed Files", "${completed.size}", Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))
        Text("By Category", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        com.edm.downloadmanager.data.db.FileCategory.values().forEach { cat ->
            val count = completed.count { it.fileCategory == cat }
            if (count > 0) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(cat.name, Modifier.weight(1f))
                    Text("$count")
                }
            }
        }
    }
}
