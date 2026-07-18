package com.edm.downloadmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edm.downloadmanager.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        SectionLabel("Appearance")
        SettingsDropdownRow(
            label = "Theme",
            value = viewModel.themeMode,
            options = listOf("SYSTEM", "LIGHT", "DARK"),
            onSelect = viewModel::setThemeMode
        )
        SettingsDropdownRow(
            label = "Language",
            value = viewModel.language,
            options = listOf("en", "bn", "es", "fr", "hi", "ar"),
            onSelect = viewModel::setLanguage
        )

        SectionLabel("Downloads")
        SettingsSliderRow(
            label = "Thread count per download: ${viewModel.threadCount}",
            value = viewModel.threadCount.toFloat(),
            range = 1f..16f,
            onChange = { viewModel.setThreadCount(it.toInt()) }
        )
        SettingsSliderRow(
            label = "Max simultaneous downloads: ${viewModel.maxSimultaneous}",
            value = viewModel.maxSimultaneous.toFloat(),
            range = 1f..10f,
            onChange = { viewModel.setMaxSimultaneous(it.toInt()) }
        )
        SettingsSliderRow(
            label = if (viewModel.speedLimitMbps == 0L) "Speed limit: Unlimited" else "Speed limit: ${viewModel.speedLimitMbps} MB/s",
            value = viewModel.speedLimitMbps.toFloat(),
            range = 0f..50f,
            onChange = { viewModel.setSpeedLimitMbps(it.toLong()) }
        )
        SwitchRow("Auto resume after interruption / reboot", viewModel.autoResumeEnabled, viewModel::setAutoResumeEnabled)

        SectionLabel("Network")
        SwitchRow("Wi-Fi only mode", viewModel.wifiOnlyMode, viewModel::setWifiOnlyMode)
        SwitchRow("Allow mobile data", viewModel.mobileDataAllowed, viewModel::setMobileDataAllowed)

        var proxyHost by remember { mutableStateOf(viewModel.proxyHost) }
        var proxyPort by remember { mutableStateOf(if (viewModel.proxyPort > 0) viewModel.proxyPort.toString() else "") }
        OutlinedTextField(proxyHost, { proxyHost = it }, label = { Text("Proxy host") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        OutlinedTextField(proxyPort, { proxyPort = it }, label = { Text("Proxy port") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Button(onClick = { viewModel.setProxy(proxyHost, proxyPort.toIntOrNull() ?: 0) }) { Text("Save proxy settings") }

        var dns by remember { mutableStateOf(viewModel.customDns) }
        OutlinedTextField(dns, { dns = it }, label = { Text("Custom DNS server (optional)") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Button(onClick = { viewModel.setCustomDns(dns) }) { Text("Save DNS setting") }

        SectionLabel("Notifications")
        SwitchRow("Enable download notifications", viewModel.notificationsEnabled, viewModel::setNotificationsEnabled)

        SectionLabel("System")
        Text("Battery optimization: for uninterrupted background downloads, disable battery optimization for EDM in Android system settings.")
        Spacer(Modifier.height(4.dp))
        Button(onClick = { /* Launches Intent.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS from the hosting Activity */ }) {
            Text("Open battery settings")
        }

        SectionLabel("Backup & Restore")
        var backupText by remember { mutableStateOf("") }
        Button(onClick = { backupText = viewModel.exportBackup() }) { Text("Export settings backup") }
        if (backupText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(backupText, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsSliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun SettingsDropdownRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, Modifier.weight(1f))
        Box {
            AssistChip(onClick = { expanded = true }, label = { Text(value) })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}
