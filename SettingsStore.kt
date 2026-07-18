package com.edm.downloadmanager.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * All Settings-screen options, backed by EncryptedSharedPreferences so values
 * such as proxy credentials are not stored in plaintext (Security requirement:
 * "Encrypted settings storage").
 */
class SettingsStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "edm_secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var themeMode: String
        get() = prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var downloadFolderUri: String?
        get() = prefs.getString(KEY_FOLDER, null)
        set(value) = prefs.edit().putString(KEY_FOLDER, value).apply()

    var threadCount: Int
        get() = prefs.getInt(KEY_THREADS, 4)
        set(value) = prefs.edit().putInt(KEY_THREADS, value.coerceIn(1, 16)).apply()

    var maxSimultaneousDownloads: Int
        get() = prefs.getInt(KEY_MAX_SIMUL, 3)
        set(value) = prefs.edit().putInt(KEY_MAX_SIMUL, value.coerceIn(1, 10)).apply()

    var speedLimitBytesPerSec: Long
        get() = prefs.getLong(KEY_SPEED_LIMIT, 0L) // 0 = unlimited
        set(value) = prefs.edit().putLong(KEY_SPEED_LIMIT, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF, value).apply()

    var autoResumeEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RESUME, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RESUME, value).apply()

    var wifiOnlyMode: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    var mobileDataAllowed: Boolean
        get() = prefs.getBoolean(KEY_MOBILE_DATA, true)
        set(value) = prefs.edit().putBoolean(KEY_MOBILE_DATA, value).apply()

    var batteryOptimizationDisabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_OPT, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_OPT, value).apply()

    var proxyHost: String?
        get() = prefs.getString(KEY_PROXY_HOST, null)
        set(value) = prefs.edit().putString(KEY_PROXY_HOST, value).apply()

    var proxyPort: Int
        get() = prefs.getInt(KEY_PROXY_PORT, 0)
        set(value) = prefs.edit().putInt(KEY_PROXY_PORT, value).apply()

    var customDnsServer: String?
        get() = prefs.getString(KEY_DNS, null)
        set(value) = prefs.edit().putString(KEY_DNS, value).apply()

    fun exportAllSettingsAsJson(): String {
        val map = prefs.all
        val sb = StringBuilder("{")
        map.entries.forEachIndexed { index, (k, v) ->
            sb.append("\"$k\":\"$v\"")
            if (index != map.size - 1) sb.append(",")
        }
        sb.append("}")
        return sb.toString() // Consumed by Settings > Backup & Restore
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_FOLDER = "download_folder"
        private const val KEY_THREADS = "thread_count"
        private const val KEY_MAX_SIMUL = "max_simultaneous"
        private const val KEY_SPEED_LIMIT = "speed_limit"
        private const val KEY_NOTIF = "notifications_enabled"
        private const val KEY_AUTO_RESUME = "auto_resume"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_MOBILE_DATA = "mobile_data_allowed"
        private const val KEY_BATTERY_OPT = "battery_opt_disabled"
        private const val KEY_PROXY_HOST = "proxy_host"
        private const val KEY_PROXY_PORT = "proxy_port"
        private const val KEY_DNS = "custom_dns"
    }
}
