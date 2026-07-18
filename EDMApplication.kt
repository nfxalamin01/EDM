package com.edm.downloadmanager

import android.app.Application
import com.edm.downloadmanager.data.db.DownloadDatabase
import com.edm.downloadmanager.data.repository.DownloadRepository
import com.edm.downloadmanager.util.NotificationHelper
import com.edm.downloadmanager.util.SettingsStore

/**
 * Simple manual DI container (kept dependency-free / no Hilt) so the project
 * builds without extra annotation-processor setup. Swap for Hilt/Koin easily
 * later since everything is exposed behind these two properties.
 */
class EDMApplication : Application() {

    val database: DownloadDatabase by lazy { DownloadDatabase.getInstance(this) }
    val downloadRepository: DownloadRepository by lazy { DownloadRepository(database.downloadDao()) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
