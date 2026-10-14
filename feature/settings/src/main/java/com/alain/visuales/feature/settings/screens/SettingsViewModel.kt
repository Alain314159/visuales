package com.alain.visuales.feature.settings.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.data.db.dao.DirectoryEntryDao
import com.alain.visuales.core.data.db.dao.DownloadDao
import com.alain.visuales.core.data.preferences.SettingsRepository
import com.alain.visuales.core.data.preferences.ThemeMode
import com.alain.visuales.core.data.preferences.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel backing the Settings screen.
 *
 * Exposes a single [StateFlow] of [UserSettings] for collection with
 * `collectAsStateWithLifecycle()`. Every UI control ends up calling one of
 * the small `setX` functions, which simply delegates to [SettingsRepository]
 * inside `viewModelScope`.
 *
 * The "Zona peligrosa" actions ([clearCatalogCache], [clearAllDownloads],
 * [resetSettings]) additionally touch the directory/download DAOs so the user
 * sees immediate UI feedback.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val directoryEntryDao: DirectoryEntryDao,
    private val downloadDao: DownloadDao
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(
            baseUrl = AppConstants.DEFAULT_BASE_URL,
            syncIntervalHours = AppConstants.DEFAULT_SYNC_INTERVAL_HOURS,
            deepScraping = false,
            enrichedMetadata = false,
            downloadFolder = AppConstants.DOWNLOAD_FOLDER_NAME,
            downloadFolderUri = null,
            connectionsPerFile = AppConstants.DEFAULT_CONNECTIONS_PER_FILE,
            maxConcurrentDownloads = AppConstants.DEFAULT_MAX_CONCURRENT_DOWNLOADS,
            wifiOnly = false,
            chargingOnly = false,
            scheduledDownloadStartHour = -1,
            scheduledDownloadEndHour = -1,
            minFreeSpaceMb = 500L,
            autoSubtitles = true,
            subtitleFontSize = 16,
            subtitleColor = 0xFFFFFFFF.toInt(),
            subtitleBackgroundColor = 0xAA000000.toInt(),
            gesturesEnabled = true,
            pipAuto = true,
            resumePlayback = true,
            defaultPlaybackSpeed = 1.0f,
            notifDownloadsActive = true,
            notifDownloadsCompleted = true,
            notifDownloadsFailed = true,
            notifCatalogUpdates = true,
            notifAppUpdates = true,
            themeMode = ThemeMode.SYSTEM,
            dynamicColor = true,
            onboardingCompleted = false,
            lastSyncShownAt = 0L
        )
    )

    // --- General ---

    fun setBaseUrl(url: String) = viewModelScope.launch { repo.setBaseUrl(url) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repo.setDynamicColor(enabled) }

    // --- Catálogo ---

    fun setSyncIntervalHours(hours: Long) = viewModelScope.launch { repo.setSyncIntervalHours(hours) }
    fun setDeepScraping(enabled: Boolean) = viewModelScope.launch { repo.setDeepScraping(enabled) }
    fun setEnrichedMetadata(enabled: Boolean) = viewModelScope.launch { repo.setEnrichedMetadata(enabled) }

    // --- Descargas ---

    fun setDownloadFolder(name: String) = viewModelScope.launch { repo.setDownloadFolder(name) }
    fun setDownloadFolderUri(uri: String?) = viewModelScope.launch { repo.setDownloadFolderUri(uri) }
    fun setConnectionsPerFile(n: Int) = viewModelScope.launch { repo.setConnectionsPerFile(n) }
    fun setMaxConcurrentDownloads(n: Int) = viewModelScope.launch { repo.setMaxConcurrentDownloads(n) }
    fun setWifiOnly(enabled: Boolean) = viewModelScope.launch { repo.setWifiOnly(enabled) }
    fun setChargingOnly(enabled: Boolean) = viewModelScope.launch { repo.setChargingOnly(enabled) }
    fun setScheduledDownload(startHour: Int, endHour: Int) = viewModelScope.launch {
        repo.setScheduledDownload(startHour, endHour)
    }
    fun setMinFreeSpaceMb(mb: Long) = viewModelScope.launch { repo.setMinFreeSpaceMb(mb) }

    // --- Subtítulos ---

    fun setAutoSubtitles(enabled: Boolean) = viewModelScope.launch { repo.setAutoSubtitles(enabled) }
    fun setSubtitleFontSize(sp: Int) = viewModelScope.launch { repo.setSubtitleFontSize(sp) }
    fun setSubtitleColor(color: Int) = viewModelScope.launch { repo.setSubtitleColor(color) }
    fun setSubtitleBackgroundColor(color: Int) = viewModelScope.launch { repo.setSubtitleBackgroundColor(color) }

    // --- Reproductor ---

    fun setGesturesEnabled(enabled: Boolean) = viewModelScope.launch { repo.setGesturesEnabled(enabled) }
    fun setPipAuto(enabled: Boolean) = viewModelScope.launch { repo.setPipAuto(enabled) }
    fun setResumePlayback(enabled: Boolean) = viewModelScope.launch { repo.setResumePlayback(enabled) }
    fun setDefaultPlaybackSpeed(speed: Float) = viewModelScope.launch { repo.setDefaultPlaybackSpeed(speed) }

    // --- Notificaciones ---

    fun setNotifDownloadsActive(enabled: Boolean) = viewModelScope.launch { repo.setNotifDownloadsActive(enabled) }
    fun setNotifDownloadsCompleted(enabled: Boolean) = viewModelScope.launch { repo.setNotifDownloadsCompleted(enabled) }
    fun setNotifDownloadsFailed(enabled: Boolean) = viewModelScope.launch { repo.setNotifDownloadsFailed(enabled) }
    fun setNotifCatalogUpdates(enabled: Boolean) = viewModelScope.launch { repo.setNotifCatalogUpdates(enabled) }
    fun setNotifAppUpdates(enabled: Boolean) = viewModelScope.launch { repo.setNotifAppUpdates(enabled) }

    // --- Zona peligrosa ---

    /**
     * Clears the cached directory-entry listing. Uses `deleteExpired` with
     * [Long.MAX_VALUE] as the cutoff, which effectively deletes every row in
     * `directory_entries` (since every TTL expiry timestamp is below MAX_VALUE).
     */
    fun clearCatalogCache() = viewModelScope.launch {
        directoryEntryDao.deleteExpired(Long.MAX_VALUE)
    }

    /**
     * Clears finished/failed/cancelled downloads. Active and queued downloads
     * are left untouched (their files are likely being written). The DAO does
     * not currently expose a `deleteAll()` query; if needed, that should be
     * added to `DownloadDao` separately.
     */
    fun clearAllDownloads() = viewModelScope.launch {
        downloadDao.clearFinished()
    }

    /**
     * Resets every persisted preference to its default value, by calling each
     * `setX` mutator with the corresponding default. The next emission of
     * [settings] will reflect the reset state.
     */
    fun resetSettings() = viewModelScope.launch {
        repo.setBaseUrl(AppConstants.DEFAULT_BASE_URL)
        repo.setSyncIntervalHours(AppConstants.DEFAULT_SYNC_INTERVAL_HOURS)
        repo.setDeepScraping(false)
        repo.setEnrichedMetadata(false)
        repo.setDownloadFolder(AppConstants.DOWNLOAD_FOLDER_NAME)
        repo.setDownloadFolderUri(null)
        repo.setConnectionsPerFile(AppConstants.DEFAULT_CONNECTIONS_PER_FILE)
        repo.setMaxConcurrentDownloads(AppConstants.DEFAULT_MAX_CONCURRENT_DOWNLOADS)
        repo.setWifiOnly(false)
        repo.setChargingOnly(false)
        repo.setScheduledDownload(-1, -1)
        repo.setMinFreeSpaceMb(500L)
        repo.setAutoSubtitles(true)
        repo.setSubtitleFontSize(16)
        repo.setSubtitleColor(0xFFFFFFFF.toInt())
        repo.setSubtitleBackgroundColor(0xAA000000.toInt())
        repo.setGesturesEnabled(true)
        repo.setPipAuto(true)
        repo.setResumePlayback(true)
        repo.setDefaultPlaybackSpeed(1.0f)
        repo.setNotifDownloadsActive(true)
        repo.setNotifDownloadsCompleted(true)
        repo.setNotifDownloadsFailed(true)
        repo.setNotifCatalogUpdates(true)
        repo.setNotifAppUpdates(true)
        repo.setThemeMode(ThemeMode.SYSTEM)
        repo.setDynamicColor(true)
    }
}
