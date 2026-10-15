package com.alain.visuales.core.data.repository

import com.alain.visuales.core.data.db.dao.LibraryDao
import com.alain.visuales.core.data.db.entity.LibraryItemEntity
import com.alain.visuales.core.data.scanner.LibraryScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for the local library (downloaded files on device).
 *
 * Provides:
 *  - observeAll() for the library screen
 *  - search() for filtering
 *  - refresh() to re-scan the filesystem and update Room
 *  - watch progress tracking (resume where the user left off)
 *  - mark watched / unwatched
 *  - delete a file from disk + DB
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val dao: LibraryDao,
    private val scanner: LibraryScanner
) {

    fun observeAll(): Flow<List<LibraryItemEntity>> = dao.observeAll()

    fun search(query: String): Flow<List<LibraryItemEntity>> = dao.search(query)

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val items = scanner.scan()
        if (items.isNotEmpty()) {
            dao.upsertAll(items)
        }
    }

    suspend fun updateWatchProgress(path: String, progressMs: Long) {
        dao.updateWatchProgress(path, progressMs, System.currentTimeMillis())
    }

    suspend fun setWatched(path: String, watched: Boolean) {
        dao.setWatched(path, watched)
    }

    suspend fun delete(path: String) {
        // Delete the file from disk if it still exists.
        try {
            val file = java.io.File(path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            // ignore — file may already be gone
        }
        dao.delete(path)
    }
}
