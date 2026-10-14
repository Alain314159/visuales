package com.alain.visuales.feature.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alain.visuales.core.data.preferences.SettingsRepository
import com.alain.visuales.core.data.preferences.UserSettings
import com.alain.visuales.core.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [PlayerScreen].
 *
 * Exposes:
 *  - [settings] — the latest [UserSettings] (stateIn of [SettingsRepository.settings]).
 *  - [updateProgress] — async wrapper for [LibraryRepository.updateWatchProgress].
 *  - [setWatched] — async wrapper for [LibraryRepository.setWatched] (marks as
 *    watched, called once when playback crosses 90%).
 *
 * The actual ExoPlayer instance lives inside the Composable (because its
 * lifecycle is tied to composition, not the ViewModel) — the VM is only
 * responsible for persistence + settings plumbing.
 *
 * Note: this VM is `@HiltViewModel` (so it survives configuration changes and
 * is shared with the player graph via `hiltViewModel()` from
 * `androidx.hilt.navigation.compose`).
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val libraryRepo: LibraryRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    fun updateProgress(path: String, progress: Long) {
        viewModelScope.launch {
            runCatching { libraryRepo.updateWatchProgress(path, progress) }
        }
    }

    fun setWatched(path: String) {
        viewModelScope.launch {
            runCatching { libraryRepo.setWatched(path, true) }
        }
    }
}
