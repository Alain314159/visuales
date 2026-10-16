package com.alain.visuales.core.data.repository

import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.common.CatalogSortMode
import com.alain.visuales.core.data.db.dao.DirectoryDao
import com.alain.visuales.core.data.db.dao.DirectoryEntryDao
import com.alain.visuales.core.data.db.dao.SyncStateDao
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.db.entity.DirectoryEntryEntity
import com.alain.visuales.core.data.db.entity.SyncStateEntity
import com.alain.visuales.core.network.api.VisualesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for catalog data.
 *
 * Two-layer strategy:
 *  - Layer 1 (synchronized): the directory tree (which folders exist) is fetched
 *    from listado.html and stored in [DirectoryEntity].
 *  - Layer 2 (lazy, cached with TTL): the contents of each folder are scraped
 *    on demand and cached in [DirectoryEntryEntity] for 24h.
 */
@Singleton
class CatalogRepository @Inject constructor(
    private val directoryDao: DirectoryDao,
    private val directoryEntryDao: DirectoryEntryDao,
    private val networkRepo: VisualesRepository,
    private val syncStateDao: SyncStateDao
) {

    // ---------------- Direct sync (bypasses WorkManager) ----------------

    /**
     * Synchronously fetch listado.html, parse it, and populate Room.
     *
     * This bypasses WorkManager entirely — called directly from a coroutine
     * in the ViewModel. Used for the initial sync after onboarding and for
     * manual "refresh" pulls.
     *
     * @param baseUrl the server base URL (default from AppConstants).
     * @return the number of directories synced, or -1 on error.
     */
    suspend fun syncNow(baseUrl: String = AppConstants.DEFAULT_BASE_URL): Int = withContext(Dispatchers.IO) {
        try {
            val remoteDirs = networkRepo.fetchDirectoryTree(baseUrl)
            val now = System.currentTimeMillis()
            val childCountMap = remoteDirs.groupingBy { it.parentPath }.eachCount()

            val existingAddedAt: Map<String, Long> = try {
                directoryDao.getAll().associate { it.path to it.addedAt }
            } catch (e: Exception) { emptyMap() }

            val entities = remoteDirs.map { rd ->
                DirectoryEntity(
                    path = rd.path,
                    url = rd.url,
                    name = rd.name,
                    parentPath = rd.parentPath,
                    depth = rd.depth,
                    section = rd.section,
                    subcategory = rd.subcategory,
                    childCount = childCountMap[rd.path] ?: 0,
                    addedAt = existingAddedAt[rd.path] ?: now,
                    lastSeenAt = now,
                    isStale = false
                )
            }

            directoryDao.syncReplaceAll(entities, now)

            val newState = SyncStateEntity(
                id = 0,
                lastSyncAt = now,
                listadoEtag = null,
                listadoLastModified = null,
                listadoSize = 0,
                totalDirectories = entities.size,
                newDirectoriesLastSync = entities.count { it.addedAt == now },
                removedDirectoriesLastSync = 0,
                lastError = null
            )
            syncStateDao.upsert(newState)
            entities.size
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            val cur = syncStateDao.get() ?: SyncStateEntity(
                id = 0, lastSyncAt = now, listadoEtag = null,
                listadoLastModified = null, listadoSize = 0,
                totalDirectories = 0, newDirectoriesLastSync = 0,
                removedDirectoriesLastSync = 0, lastError = null
            )
            syncStateDao.upsert(cur.copy(lastSyncAt = now, lastError = e.message ?: e.toString()))
            -1
        }
    }

    // ---------------- Layer 1: directory tree ----------------

    fun observeChildren(parentPath: String): Flow<List<DirectoryEntity>> =
        directoryDao.children(parentPath)

    suspend fun getDirectory(path: String): DirectoryEntity? =
        directoryDao.getByPath(path)

    fun observeRecentlyAdded(limit: Int = 50): Flow<List<DirectoryEntity>> =
        directoryDao.recentlyAdded(limit)

    fun observeSections(): Flow<Map<String, Int>> =
        directoryDao.sectionCounts().map { list ->
            list.associate { it.section to it.count }
        }

    fun searchDirectories(query: String): Flow<List<DirectoryEntity>> =
        directoryDao.search(query)

    fun observeTopLevel(): Flow<List<DirectoryEntity>> =
        directoryDao.children("/")

    // ---------------- Layer 2: directory contents ----------------

    fun observeEntries(parentPath: String): Flow<List<DirectoryEntryEntity>> =
        directoryEntryDao.entriesFor(parentPath)

    /**
     * Fetch the contents of a directory. If cached and fresh, return cache and
     * trigger a background refresh.
     *
     * @param sortMode native Apache autoindex sort (?C=X;O=Y). When non-null,
     *   the sort query param is appended to the URL before scraping.
     */
    fun fetchDirectoryContents(
        dirPath: String,
        dirUrl: String,
        forceRefresh: Boolean = false,
        sortMode: CatalogSortMode = CatalogSortMode.DEFAULT
    ): Flow<FetchResult<List<DirectoryEntryEntity>>> = flow {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val freshCount = directoryEntryDao.countFresh(dirPath, now)
            if (freshCount > 0) {
                val cached = directoryEntryDao.snapshotFor(dirPath)
                emit(FetchResult.Success(cached, fromCache = true))
            }
        }
        // Network fetch — append native sort query param to the URL.
        val sortedUrl = if (sortMode == CatalogSortMode.DEFAULT) {
            dirUrl
        } else {
            val sep = if (dirUrl.contains('?')) '&' else '?'
            "$dirUrl${sep}${sortMode.queryParam}"
        }
        try {
            val remoteEntries = networkRepo.fetchDirectoryContents(sortedUrl)
            val entities = remoteEntries.map {
                DirectoryEntryEntity(
                    parentPath = dirPath,
                    name = it.name,
                    url = it.url,
                    path = it.path,
                    isDirectory = it.isDirectory,
                    type = it.type,
                    size = it.size,
                    modifiedAt = it.modifiedAt,
                    extension = if (it.isDirectory) "" else it.name.substringAfterLast('.', "").lowercase(),
                    fetchedAt = now,
                    ttlExpiresAt = now + AppConstants.DEFAULT_DIRECTORY_TTL_MS
                )
            }
            directoryEntryDao.deleteFor(dirPath)
            directoryEntryDao.upsertAll(entities)
            emit(FetchResult.Success(entities, fromCache = false))
        } catch (e: Exception) {
            val cached = directoryEntryDao.snapshotFor(dirPath)
            if (cached.isNotEmpty()) {
                emit(FetchResult.Success(cached, fromCache = true))
            } else {
                emit(FetchResult.Error(e))
            }
        }
    }

    suspend fun clearExpiredCache() {
        directoryEntryDao.deleteExpired(System.currentTimeMillis())
    }
}

sealed interface FetchResult<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean) : FetchResult<T>
    data class Error(val exception: Throwable) : FetchResult<Nothing>
    data object Loading : FetchResult<Nothing>
}
