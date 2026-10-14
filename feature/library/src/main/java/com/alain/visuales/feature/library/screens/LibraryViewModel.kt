package com.alain.visuales.feature.library.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alain.visuales.core.data.db.entity.LibraryItemEntity
import com.alain.visuales.core.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Sort mode for the library grid.
 *
 * - [DATE_ADDED]  : newest items first (descending [LibraryItemEntity.addedAt]).
 * - [NAME]        : alphabetical, case-insensitive.
 * - [SIZE]        : largest files first (descending [LibraryItemEntity.size]).
 * - [LAST_PLAYED] : most-recently played first (descending [LibraryItemEntity.lastPlayedAt],
 *                   nulls last).
 */
enum class SortMode { DATE_ADDED, NAME, SIZE, LAST_PLAYED }

/**
 * ViewModel for the Library screen.
 *
 * Exposes a single derived [items] flow that combines the live
 * [LibraryRepository.observeAll] stream with the current [searchQuery] and
 * [sortMode]. Filtering and sorting are applied on every emission so the UI
 * never has to re-derive them.
 *
 * Mutations (refresh, toggle watched, delete, play) are forwarded to the
 * repository on `viewModelScope` so the UI never has to launch coroutines
 * itself.
 *
 * @param repo the single source of truth for library state on disk + in Room.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: LibraryRepository
) : ViewModel() {

    /** Current search query typed by the user. Empty string = no filter. */
    val searchQuery = MutableStateFlow("")

    /** Current sort mode selected from the overflow menu. */
    val sortMode = MutableStateFlow(SortMode.DATE_ADDED)

    /**
     * True while [refresh] is running (filesystem re-scan in progress).
     * Drives the [androidx.compose.material3.pullrefresh.PullToRefreshBox]
     * indicator.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * The filtered + sorted list of library items. Computed by
     * [applyFiltersAndSort] on every emission of the upstream triple.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<LibraryItemEntity>> = combine(
        repo.observeAll(),
        searchQuery,
        sortMode
    ) { all, q, sort ->
        applyFiltersAndSort(all, q, sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList()
    )

    // ---- Mutators ---------------------------------------------------

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun setSortMode(mode: SortMode) {
        sortMode.value = mode
    }

    /**
     * Re-scan the filesystem via [LibraryRepository.refresh] and update Room.
     * Flips [isRefreshing] on for the duration of the call (plus a small
     * minimum delay so the pull-to-refresh indicator is visible).
     */
    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(MIN_REFRESH_SPIN_MS)
                repo.refresh()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleWatched(item: LibraryItemEntity) = viewModelScope.launch {
        repo.setWatched(item.path, !item.isWatched)
    }

    fun delete(item: LibraryItemEntity) = viewModelScope.launch {
        repo.delete(item.path)
    }

    /**
     * Open [item] in an external video player via [FileProvider] +
     * [Intent.ACTION_VIEW].
     *
     * The file's on-disk path is used directly (it's already absolute).
     * If the file no longer exists, or no activity can handle `video/*`,
     * a short Toast is shown.
     *
     * @param context any context; the function uses [Context.getApplicationContext].
     */
    fun play(context: Context, item: LibraryItemEntity) {
        val appContext = context.applicationContext
        val file = File(item.path)
        if (!file.exists()) {
            Toast.makeText(appContext, "Archivo no encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        val authority = "${appContext.packageName}.fileprovider"
        val uri: Uri = try {
            FileProvider.getUriForFile(appContext, authority, file)
        } catch (_: IllegalArgumentException) {
            // File lives outside the FileProvider's declared paths
            // (e.g. on a removable storage path the provider doesn't expose).
            Toast.makeText(appContext, "No se puede abrir el archivo", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(appContext, "No hay reproductor de video", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Helpers ----------------------------------------------------

    /**
     * Pure function: apply the current search query (case-insensitive
     * substring match against the file's display name) and then sort by the
     * selected [SortMode].
     *
     * Pulled out of the combine() so it can be unit-tested in isolation.
     */
    private fun applyFiltersAndSort(
        all: List<LibraryItemEntity>,
        query: String,
        sort: SortMode
    ): List<LibraryItemEntity> {
        val filtered = if (query.isBlank()) {
            all
        } else {
            val q = query.trim().lowercase()
            all.filter { it.name.lowercase().contains(q) }
        }
        return when (sort) {
            SortMode.DATE_ADDED -> filtered.sortedByDescending { it.addedAt }
            SortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortMode.SIZE -> filtered.sortedByDescending { it.size }
            SortMode.LAST_PLAYED -> filtered.sortedByDescending { it.lastPlayedAt ?: 0L }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        /** Minimum visible duration of the pull-to-refresh spinner, in ms. */
        private const val MIN_REFRESH_SPIN_MS = 500L
    }
}
