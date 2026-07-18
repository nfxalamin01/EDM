package com.edm.downloadmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("EDM", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Fast. Smart. Reliable.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("Version 1.0.0", style = MaterialTheme.typography.labelSmall)

        Spacer(Modifier.height(32.dp))
        Text(
            "EDM (Extreme Download Manager) is a high-speed, AI-era download manager " +
                "for Android with segmented multi-thread downloading, smart retry, " +
                "background downloads, and complete queue and file management.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Text("Built with Kotlin, Jetpack Compose, Room, WorkManager and OkHttp.", style = MaterialTheme.typography.labelSmall)
    }
}
