package com.edm.downloadmanager.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.edm.downloadmanager.EDMApplication

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as EDMApplication).settingsStore

    var themeMode by mutableStateOf(settings.themeMode); private set
    var language by mutableStateOf(settings.language); private set
    var threadCount by mutableStateOf(settings.threadCount); private set
    var maxSimultaneous by mutableStateOf(settings.maxSimultaneousDownloads); private set
    var speedLimitMbps by mutableStateOf(settings.speedLimitBytesPerSec / (1024 * 1024)); private set
    var notificationsEnabled by mutableStateOf(settings.notificationsEnabled); private set
    var autoResumeEnabled by mutableStateOf(settings.autoResumeEnabled); private set
    var wifiOnlyMode by mutableStateOf(settings.wifiOnlyMode); private set
    var mobileDataAllowed by mutableStateOf(settings.mobileDataAllowed); private set
    var proxyHost by mutableStateOf(settings.proxyHost ?: ""); private set
    var proxyPort by mutableStateOf(settings.proxyPort); private set
    var customDns by mutableStateOf(settings.customDnsServer ?: ""); private set

    fun setThemeMode(value: String) { themeMode = value; settings.themeMode = value }
    fun setLanguage(value: String) { language = value; settings.language = value }
    fun setThreadCount(value: Int) { threadCount = value; settings.threadCount = value }
    fun setMaxSimultaneous(value: Int) { maxSimultaneous = value; settings.maxSimultaneousDownloads = value }
    fun setSpeedLimitMbps(value: Long) { speedLimitMbps = value; settings.speedLimitBytesPerSec = value * 1024 * 1024 }
    fun setNotificationsEnabled(value: Boolean) { notificationsEnabled = value; settings.notificationsEnabled = value }
    fun setAutoResumeEnabled(value: Boolean) { autoResumeEnabled = value; settings.autoResumeEnabled = value }
    fun setWifiOnlyMode(value: Boolean) { wifiOnlyMode = value; settings.wifiOnlyMode = value }
    fun setMobileDataAllowed(value: Boolean) { mobileDataAllowed = value; settings.mobileDataAllowed = value }
    fun setProxy(host: String, port: Int) { proxyHost = host; proxyPort = port; settings.proxyHost = host; settings.proxyPort = port }
    fun setCustomDns(value: String) { customDns = value; settings.customDnsServer = value }

    fun exportBackup(): String = settings.exportAllSettingsAsJson()
}
