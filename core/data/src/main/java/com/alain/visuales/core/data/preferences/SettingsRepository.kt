package com.alain.visuales.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alain.visuales.core.common.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User settings, persisted with DataStore.
 *
 * Backs: server URL, sync interval, download config, subtitle/replicant prefs,
 * theme, onboarding state, etc.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val settings: Flow<UserSettings> = dataStore.data.map { p ->
        UserSettings(
            baseUrl = p[KEY_BASE_URL] ?: AppConstants.DEFAULT_BASE_URL,
            syncIntervalHours = p[KEY_SYNC_INTERVAL_HOURS] ?: AppConstants.DEFAULT_SYNC_INTERVAL_HOURS,
            deepScraping = p[KEY_DEEP_SCRAPING] ?: false,
            enrichedMetadata = p[KEY_ENRICHED_METADATA] ?: false,
            downloadFolder = p[KEY_DOWNLOAD_FOLDER] ?: "Visuales",
            downloadFolderUri = p[KEY_DOWNLOAD_FOLDER_URI],
            connectionsPerFile = p[KEY_CONNECTIONS_PER_FILE] ?: AppConstants.DEFAULT_CONNECTIONS_PER_FILE,
            maxConcurrentDownloads = p[KEY_MAX_CONCURRENT] ?: AppConstants.DEFAULT_MAX_CONCURRENT_DOWNLOADS,
            wifiOnly = p[KEY_WIFI_ONLY] ?: false,
            chargingOnly = p[KEY_CHARGING_ONLY] ?: false,
            scheduledDownloadStartHour = p[KEY_SCHEDULED_START_HOUR] ?: -1,
            scheduledDownloadEndHour = p[KEY_SCHEDULED_END_HOUR] ?: -1,
            minFreeSpaceMb = p[KEY_MIN_FREE_SPACE_MB] ?: 500L,
            autoSubtitles = p[KEY_AUTO_SUBTITLES] ?: true,
            subtitleFontSize = p[KEY_SUBTITLE_FONT_SIZE] ?: 16,
            subtitleColor = p[KEY_SUBTITLE_COLOR] ?: 0xFFFFFFFF.toInt(),
            subtitleBackgroundColor = p[KEY_SUBTITLE_BG_COLOR] ?: 0xAA000000.toInt(),
            gesturesEnabled = p[KEY_GESTURES_ENABLED] ?: true,
            pipAuto = p[KEY_PIP_AUTO] ?: true,
            resumePlayback = p[KEY_RESUME_PLAYBACK] ?: true,
            defaultPlaybackSpeed = p[KEY_DEFAULT_SPEED] ?: 1.0f,
            notifDownloadsActive = p[KEY_NOTIF_DOWNLOADS_ACTIVE] ?: true,
            notifDownloadsCompleted = p[KEY_NOTIF_DOWNLOADS_COMPLETED] ?: true,
            notifDownloadsFailed = p[KEY_NOTIF_DOWNLOADS_FAILED] ?: true,
            notifCatalogUpdates = p[KEY_NOTIF_CATALOG_UPDATES] ?: true,
            notifAppUpdates = p[KEY_NOTIF_APP_UPDATES] ?: true,
            themeMode = ThemeMode.valueOf(p[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name),
            dynamicColor = p[KEY_DYNAMIC_COLOR] ?: true,
            onboardingCompleted = p[KEY_ONBOARDING_COMPLETED] ?: false,
            lastSyncShownAt = p[KEY_LAST_SYNC_SHOWN_AT] ?: 0L,
            embyEnabled = p[KEY_EMBY_ENABLED] ?: true,
            embyToken = p[KEY_EMBY_TOKEN],
            embyUserId = p[KEY_EMBY_USER_ID],
            embyDeviceId = p[KEY_EMBY_DEVICE_ID]
        )
    }

    // --- Mutators (each suspends and updates the underlying DataStore) ---

    suspend fun setBaseUrl(url: String) = dataStore.edit { it[KEY_BASE_URL] = url.trimEnd('/') }
    suspend fun setSyncIntervalHours(hours: Long) = dataStore.edit { it[KEY_SYNC_INTERVAL_HOURS] = hours }
    suspend fun setDeepScraping(enabled: Boolean) = dataStore.edit { it[KEY_DEEP_SCRAPING] = enabled }
    suspend fun setEnrichedMetadata(enabled: Boolean) = dataStore.edit { it[KEY_ENRICHED_METADATA] = enabled }
    suspend fun setDownloadFolder(name: String) = dataStore.edit { it[KEY_DOWNLOAD_FOLDER] = name }
    suspend fun setDownloadFolderUri(uri: String?) = dataStore.edit { if (uri == null) it.remove(KEY_DOWNLOAD_FOLDER_URI) else it[KEY_DOWNLOAD_FOLDER_URI] = uri }
    suspend fun setConnectionsPerFile(n: Int) = dataStore.edit { it[KEY_CONNECTIONS_PER_FILE] = n.coerceIn(1, 32) }
    suspend fun setMaxConcurrentDownloads(n: Int) = dataStore.edit { it[KEY_MAX_CONCURRENT] = n.coerceIn(1, 5) }
    suspend fun setWifiOnly(enabled: Boolean) = dataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    suspend fun setChargingOnly(enabled: Boolean) = dataStore.edit { it[KEY_CHARGING_ONLY] = enabled }
    suspend fun setScheduledDownload(startHour: Int, endHour: Int) = dataStore.edit {
        it[KEY_SCHEDULED_START_HOUR] = startHour
        it[KEY_SCHEDULED_END_HOUR] = endHour
    }
    suspend fun setMinFreeSpaceMb(mb: Long) = dataStore.edit { it[KEY_MIN_FREE_SPACE_MB] = mb }
    suspend fun setAutoSubtitles(enabled: Boolean) = dataStore.edit { it[KEY_AUTO_SUBTITLES] = enabled }
    suspend fun setSubtitleFontSize(sp: Int) = dataStore.edit { it[KEY_SUBTITLE_FONT_SIZE] = sp.coerceIn(8, 48) }
    suspend fun setSubtitleColor(color: Int) = dataStore.edit { it[KEY_SUBTITLE_COLOR] = color }
    suspend fun setSubtitleBackgroundColor(color: Int) = dataStore.edit { it[KEY_SUBTITLE_BG_COLOR] = color }
    suspend fun setGesturesEnabled(enabled: Boolean) = dataStore.edit { it[KEY_GESTURES_ENABLED] = enabled }
    suspend fun setPipAuto(enabled: Boolean) = dataStore.edit { it[KEY_PIP_AUTO] = enabled }
    suspend fun setResumePlayback(enabled: Boolean) = dataStore.edit { it[KEY_RESUME_PLAYBACK] = enabled }
    suspend fun setDefaultPlaybackSpeed(speed: Float) = dataStore.edit { it[KEY_DEFAULT_SPEED] = speed.coerceIn(0.5f, 2.0f) }
    suspend fun setNotifDownloadsActive(enabled: Boolean) = dataStore.edit { it[KEY_NOTIF_DOWNLOADS_ACTIVE] = enabled }
    suspend fun setNotifDownloadsCompleted(enabled: Boolean) = dataStore.edit { it[KEY_NOTIF_DOWNLOADS_COMPLETED] = enabled }
    suspend fun setNotifDownloadsFailed(enabled: Boolean) = dataStore.edit { it[KEY_NOTIF_DOWNLOADS_FAILED] = enabled }
    suspend fun setNotifCatalogUpdates(enabled: Boolean) = dataStore.edit { it[KEY_NOTIF_CATALOG_UPDATES] = enabled }
    suspend fun setNotifAppUpdates(enabled: Boolean) = dataStore.edit { it[KEY_NOTIF_APP_UPDATES] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    suspend fun setOnboardingCompleted(completed: Boolean) = dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    suspend fun markSyncShown(now: Long = System.currentTimeMillis()) = dataStore.edit { it[KEY_LAST_SYNC_SHOWN_AT] = now }

    // --- Emby integration ---

    /** Whether Emby lookups (poster / overview / genres) are enabled. */
    suspend fun setEmbyEnabled(enabled: Boolean) = dataStore.edit { it[KEY_EMBY_ENABLED] = enabled }

    /** Persist the Emby access token. `null` clears the value. */
    suspend fun setEmbyToken(token: String?) = dataStore.edit {
        if (token == null) it.remove(KEY_EMBY_TOKEN) else it[KEY_EMBY_TOKEN] = token
    }

    /** Persist the Emby user id. `null` clears the value. */
    suspend fun setEmbyUserId(id: String?) = dataStore.edit {
        if (id == null) it.remove(KEY_EMBY_USER_ID) else it[KEY_EMBY_USER_ID] = id
    }

    /** Persist the per-install device id (generated once). */
    suspend fun setEmbyDeviceId(id: String) = dataStore.edit { it[KEY_EMBY_DEVICE_ID] = id }

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_SYNC_INTERVAL_HOURS = longPreferencesKey("sync_interval_hours")
        private val KEY_DEEP_SCRAPING = booleanPreferencesKey("deep_scraping")
        private val KEY_ENRICHED_METADATA = booleanPreferencesKey("enriched_metadata")
        private val KEY_DOWNLOAD_FOLDER = stringPreferencesKey("download_folder")
        private val KEY_DOWNLOAD_FOLDER_URI = stringPreferencesKey("download_folder_uri")
        private val KEY_CONNECTIONS_PER_FILE = intPreferencesKey("connections_per_file")
        private val KEY_MAX_CONCURRENT = intPreferencesKey("max_concurrent_downloads")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val KEY_CHARGING_ONLY = booleanPreferencesKey("charging_only")
        private val KEY_SCHEDULED_START_HOUR = intPreferencesKey("scheduled_start_hour")
        private val KEY_SCHEDULED_END_HOUR = intPreferencesKey("scheduled_end_hour")
        private val KEY_MIN_FREE_SPACE_MB = longPreferencesKey("min_free_space_mb")
        private val KEY_AUTO_SUBTITLES = booleanPreferencesKey("auto_subtitles")
        private val KEY_SUBTITLE_FONT_SIZE = intPreferencesKey("subtitle_font_size")
        private val KEY_SUBTITLE_COLOR = intPreferencesKey("subtitle_color")
        private val KEY_SUBTITLE_BG_COLOR = intPreferencesKey("subtitle_bg_color")
        private val KEY_GESTURES_ENABLED = booleanPreferencesKey("gestures_enabled")
        private val KEY_PIP_AUTO = booleanPreferencesKey("pip_auto")
        private val KEY_RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        private val KEY_DEFAULT_SPEED = floatPreferencesKey("default_speed")
        private val KEY_NOTIF_DOWNLOADS_ACTIVE = booleanPreferencesKey("notif_downloads_active")
        private val KEY_NOTIF_DOWNLOADS_COMPLETED = booleanPreferencesKey("notif_downloads_completed")
        private val KEY_NOTIF_DOWNLOADS_FAILED = booleanPreferencesKey("notif_downloads_failed")
        private val KEY_NOTIF_CATALOG_UPDATES = booleanPreferencesKey("notif_catalog_updates")
        private val KEY_NOTIF_APP_UPDATES = booleanPreferencesKey("notif_app_updates")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_LAST_SYNC_SHOWN_AT = longPreferencesKey("last_sync_shown_at")
        private val KEY_EMBY_ENABLED = booleanPreferencesKey("emby_enabled")
        private val KEY_EMBY_TOKEN = stringPreferencesKey("emby_token")
        private val KEY_EMBY_USER_ID = stringPreferencesKey("emby_user_id")
        private val KEY_EMBY_DEVICE_ID = stringPreferencesKey("emby_device_id")
    }
}

data class UserSettings(
    val baseUrl: String,
    val syncIntervalHours: Long,
    val deepScraping: Boolean,
    val enrichedMetadata: Boolean,
    val downloadFolder: String,
    val downloadFolderUri: String?,
    val connectionsPerFile: Int,
    val maxConcurrentDownloads: Int,
    val wifiOnly: Boolean,
    val chargingOnly: Boolean,
    val scheduledDownloadStartHour: Int,  // -1 = disabled
    val scheduledDownloadEndHour: Int,
    val minFreeSpaceMb: Long,
    val autoSubtitles: Boolean,
    val subtitleFontSize: Int,
    val subtitleColor: Int,
    val subtitleBackgroundColor: Int,
    val gesturesEnabled: Boolean,
    val pipAuto: Boolean,
    val resumePlayback: Boolean,
    val defaultPlaybackSpeed: Float,
    val notifDownloadsActive: Boolean,
    val notifDownloadsCompleted: Boolean,
    val notifDownloadsFailed: Boolean,
    val notifCatalogUpdates: Boolean,
    val notifAppUpdates: Boolean,
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val onboardingCompleted: Boolean,
    val lastSyncShownAt: Long,
    /** Whether Emby lookups (poster / overview / genres) are enabled. */
    val embyEnabled: Boolean = true,
    /** Cached Emby access token (cleared on logout). */
    val embyToken: String? = null,
    /** Cached Emby user id. */
    val embyUserId: String? = null,
    /** Per-install device id used for X-Emby-Device-Id. */
    val embyDeviceId: String? = null
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
