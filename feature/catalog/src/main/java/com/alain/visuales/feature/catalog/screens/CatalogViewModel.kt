package com.alain.visuales.feature.catalog.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alain.visuales.core.common.CatalogSections
import com.alain.visuales.core.common.CatalogSortMode
import com.alain.visuales.core.data.db.dao.SyncStateDao
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.db.entity.DirectoryEntryEntity
import com.alain.visuales.core.data.db.entity.EmbyItemEntity
import com.alain.visuales.core.data.db.entity.SyncStateEntity
import com.alain.visuales.core.data.preferences.SettingsRepository
import com.alain.visuales.core.data.repository.CatalogRepository
import com.alain.visuales.core.data.repository.DownloadRepository
import com.alain.visuales.core.data.repository.EmbyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds state for the three catalog screens (Home, Browse, Search).
 *
 * Hilt scopes one instance per NavBackStackEntry (via `hiltViewModel()`),
 * so each screen owns its own VM. BrowseScreen seeds [currentPath] from the
 * navigation argument via [navigateTo] in a `LaunchedEffect`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repo: CatalogRepository,
    private val syncStateDao: SyncStateDao,
    private val downloadRepo: DownloadRepository,
    private val embyRepo: EmbyRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    // -------- Browse state --------

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    /** Resolve the [DirectoryEntity] for the current path (suspended lookup). */
    val currentDirectory: StateFlow<DirectoryEntity?> = _currentPath
        .flatMapLatest { path -> flow { emit(repo.getDirectory(path)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Sub-directories of [currentPath] (Layer 1: synced tree). */
    val currentChildren: StateFlow<List<DirectoryEntity>> = _currentPath
        .flatMapLatest { repo.observeChildren(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** File entries inside [currentPath] (Layer 2: scraped on demand, TTL cached). */
    val currentEntries: StateFlow<List<DirectoryEntryEntity>> = _currentPath
        .flatMapLatest { repo.observeEntries(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -------- Home state --------

    /** Most recently added directories across all sections (used for the hero card). */
    val recentlyAdded: StateFlow<List<DirectoryEntity>> = repo
        .observeRecentlyAdded(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Top-level sections (children of "/"). */
    val topLevel: StateFlow<List<DirectoryEntity>> = repo
        .observeTopLevel()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Per-section carousels. For "Recientes" we use [CatalogRepository.observeRecentlyAdded];
     * for the other featured sections we observe children of `"/<section>/"` since the
     * section name is the first path segment.
     */
    val homeSections: StateFlow<Map<String, List<DirectoryEntity>>> = combine(
        CatalogSections.FEATURED.map { section ->
            if (section == CatalogSections.RECIENTES) {
                repo.observeRecentlyAdded(20)
            } else {
                repo.observeChildren("/$section")
            }
        }
    ) { arrays ->
        CatalogSections.FEATURED.mapIndexed { i, s -> s to arrays[i] }.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val syncState: StateFlow<SyncStateEntity?> = syncStateDao.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // -------- Emby enrichment --------

    /**
     * Per-directory-path Emby metadata. Updated asynchronously as the UI asks
     * for lookups via [requestEmbyLookup]. `null` means "not yet known / no
     * match"; the UI should show a placeholder poster in that case.
     */
    private val _embyPosters = MutableStateFlow<Map<String, EmbyItemEntity?>>(emptyMap())
    val embyPosters: StateFlow<Map<String, EmbyItemEntity?>> = _embyPosters.asStateFlow()

    /** Paths already enqueued for an Emby lookup in this VM lifetime. */
    private val embyLookedUp = mutableSetOf<String>()

    /**
     * Fire-and-forget: ask the Emby layer to enrich [path]. Reads from cache
     * first (one-shot), falls back to a network search + cache write if there
     * is no fresh entry. Updates [embyPosters] as data arrives. No-op if Emby
     * is disabled in settings.
     */
    fun requestEmbyLookup(path: String) {
        if (!embyLookedUp.add(path)) return  // already in flight
        viewModelScope.launch {
            // Honor the global toggle.
            val enabled = settingsRepo.settings.first().embyEnabled
            if (!enabled) return@launch

            // One-shot cache read first (cheaper than a Room flow subscribe).
            val cached = runCatching { embyRepo.lookupByPath(path).first() }.getOrNull()
            if (cached != null) {
                _embyPosters.value = _embyPosters.value + (path to cached)
                return@launch
            }
            // Cache miss → network search + write.
            val refreshed = runCatching { embyRepo.refreshByPath(path) }.getOrNull()
            _embyPosters.value = _embyPosters.value + (path to refreshed)
        }
    }

    /** Convenience: build a Coil-loadable poster URL for a cached Emby item. */
    fun posterUrlFor(path: String): String? {
        val entity = _embyPosters.value[path] ?: return null
        return embyRepo.posterUrl(entity.itemId, entity.posterTag)
    }

    // -------- Search state --------

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Search results with 300 ms debounce, case-insensitive (LIKE query in DAO). */
    val searchResults: StateFlow<List<DirectoryEntity>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) emptyFlow()
            else repo.searchDirectories(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -------- Snackbar events --------

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    // -------- Navigation / actions --------

    fun navigateTo(path: String) {
        val normalized = if (path.endsWith("/") || path == "/") path else "$path/"
        if (_currentPath.value == normalized) return
        _currentPath.value = normalized
        triggerFetch(normalized)
    }

    /** Pop one level. Returns `false` if already at root (caller may pop the back stack). */
    fun navigateBack(): Boolean {
        val cur = _currentPath.value.trimEnd('/')
        if (cur.isEmpty() || cur == "/") return false
        val parent = cur.substringBeforeLast('/', "").ifEmpty { "/" }
        val parentPath = if (parent.endsWith("/")) parent else "$parent/"
        _currentPath.value = parentPath
        triggerFetch(parentPath)
        return true
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    // -------- Sort mode (native Apache autoindex ?C=X;O=Y) --------
    private val _sortMode = MutableStateFlow(CatalogSortMode.DEFAULT)
    val sortMode: StateFlow<CatalogSortMode> = _sortMode.asStateFlow()

    fun setSortMode(mode: CatalogSortMode) {
        _sortMode.value = mode
        // Re-fetch with the new sort so the UI updates.
        forceRefresh()
    }

    /**
     * Force-refresh the current folder: clears the cached entries (forces a network
     * scrape) and re-fetches via [CatalogRepository.fetchDirectoryContents].
     */
    fun forceRefresh() {
        viewModelScope.launch {
            val path = _currentPath.value
            val dir = repo.getDirectory(path) ?: return@launch
            repo.fetchDirectoryContents(path, dir.url, forceRefresh = true, sortMode = _sortMode.value)
                .collect { /* state reflects via observeEntries */ }
        }
    }

    /** Trigger a non-forced fetch (uses cache if fresh, otherwise scrapes). */
    fun triggerFetch(path: String = _currentPath.value) {
        viewModelScope.launch {
            val dir = repo.getDirectory(path) ?: return@launch
            repo.fetchDirectoryContents(path, dir.url, forceRefresh = false).collect { /* no-op */ }
        }
    }

    /** Enqueue a single file download. */
    fun enqueueDownload(entry: DirectoryEntryEntity) {
        viewModelScope.launch {
            val section = entry.parentPath.trim('/').substringBefore('/', "").ifBlank { null }
            // Try to find a sibling .srt subtitle in the same folder (if we have the listing).
            val siblingSubtitleUrl = findSiblingSubtitleUrl(entry)
            downloadRepo.enqueue(
                url = entry.url,
                name = entry.name,
                size = entry.size,
                subtitleUrl = siblingSubtitleUrl,
                section = section
            )
            _snackbarMessage.value = "Añadido a la cola: ${entry.name}"
        }
    }

    /** Enqueue every video file in [entries]. Used by the "Descargar todos" FAB. */
    fun enqueueAllVideos(entries: List<DirectoryEntryEntity>) {
        val videos = entries.filter { it.type == "VIDEO" && !it.isDirectory }
        if (videos.isEmpty()) return
        viewModelScope.launch {
            val items = videos.map { entry ->
                val section = entry.parentPath.trim('/').substringBefore('/', "").ifBlank { null }
                DownloadRepository.EnqueueItem(
                    url = entry.url,
                    name = entry.name,
                    size = entry.size,
                    subtitleUrl = findSiblingSubtitleUrl(entry),
                    section = section
                )
            }
            val ids = downloadRepo.enqueueBatch(items)
            _snackbarMessage.value = "Añadido a la cola: ${ids.size} archivo(s)"
        }
    }

    /**
     * Find the URL of a sibling .srt subtitle for [video].
     *
     * Looks in the current cached directory listing for an .srt file whose
     * basename matches the video's basename. Returns the absolute URL or null.
     */
    private suspend fun findSiblingSubtitleUrl(video: DirectoryEntryEntity): String? {
        val videoBase = video.name.substringBeforeLast('.').lowercase()
        if (videoBase.isBlank()) return null
        // Look at the entries cached for the same parent path.
        val siblings = repo.observeEntries(video.parentPath).first()
        return siblings
            .firstOrNull { e ->
                !e.isDirectory &&
                    e.type == "SUBTITLE" &&
                    e.name.substringBeforeLast('.').lowercase() == videoBase
            }?.url
    }

    /**
     * Pull-to-refresh on Home: clears the expired entry cache and re-fetches each
     * top-level section's contents. The full listado.html sync is left to the
     * `ListadoSyncWorker` (WorkManager) — this only refreshes the Layer-2 listings.
     */
    fun triggerHomeRefresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repo.clearExpiredCache()
                topLevel.value.forEach { dir ->
                    runCatching {
                        repo.fetchDirectoryContents(dir.path, dir.url, forceRefresh = true)
                            .collect { /* drain */ }
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
