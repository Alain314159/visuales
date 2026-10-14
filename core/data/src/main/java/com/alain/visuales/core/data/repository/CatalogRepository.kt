package com.alain.visuales.core.data.repository

import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.data.db.dao.DirectoryDao
import com.alain.visuales.core.data.db.dao.DirectoryEntryDao
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.db.entity.DirectoryEntryEntity
import com.alain.visuales.core.network.api.VisualesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
    private val networkRepo: VisualesRepository
) {

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
     */
    fun fetchDirectoryContents(
        dirPath: String,
        dirUrl: String,
        forceRefresh: Boolean = false
    ): Flow<FetchResult<List<DirectoryEntryEntity>>> = flow {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val freshCount = directoryEntryDao.countFresh(dirPath, now)
            if (freshCount > 0) {
                val cached = directoryEntryDao.snapshotFor(dirPath)
                emit(FetchResult.Success(cached, fromCache = true))
            }
        }
        // Network fetch.
        try {
            val remoteEntries = networkRepo.fetchDirectoryContents(dirUrl)
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
            // If we already emitted cache, just emit error (UI already has data).
            // Otherwise emit error.
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
