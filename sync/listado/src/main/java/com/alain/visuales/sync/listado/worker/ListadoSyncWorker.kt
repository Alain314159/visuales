package com.alain.visuales.sync.listado.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.data.db.dao.DirectoryDao
import com.alain.visuales.core.data.db.dao.SyncStateDao
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.db.entity.SyncStateEntity
import com.alain.visuales.core.network.api.VisualesRepository
import com.alain.visuales.core.network.model.ResourceMetadata
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that synchronizes the catalog tree from listado.html.
 *
 * Strategy:
 *  1. Issue a HEAD to listado.html and compare ETag / Last-Modified with the
 *     last sync. If unchanged, exit early (304-style).
 *  2. If changed: download full HTML, parse with ListadoHtmlParser, compute
 *     diff against the DB, and upsert / mark stale / delete.
 *  3. Update SyncStateEntity with new ETag/Last-Modified and counters.
 *
 * This worker never scrapes per-folder contents — that's done lazily by
 * CatalogRepository when the user opens a folder.
 */
@HiltWorker
class ListadoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val networkRepo: VisualesRepository,
    private val directoryDao: DirectoryDao,
    private val syncStateDao: SyncStateDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val baseUrl = AppConstants.DEFAULT_BASE_URL
            val listadoUrl = baseUrl.trimEnd('/') + AppConstants.LISTADO_HTML_PATH

            // 1. HEAD to check ETag / Last-Modified.
            val meta = networkRepo.headResource(listadoUrl)
            val prevState = syncStateDao.get()

            val unchanged = prevState != null &&
                meta.etag != null && meta.etag == prevState.listadoEtag &&
                meta.lastModified != null && meta.lastModified == prevState.listadoLastModified

            if (unchanged) {
                // Nothing to do.
                return Result.success()
            }

            // 2. Fetch and parse.
            val remoteDirs = networkRepo.fetchDirectoryTree(baseUrl)
            val now = System.currentTimeMillis()

            // Compute child counts from the tree (single pass).
            val childCountMap = remoteDirs.groupingBy { it.parentPath }.eachCount()

            // Bulk fetch existing addedAt timestamps to preserve them (1 query instead of 30k).
            val existingAddedAt: Map<String, Long> = try {
                directoryDao.getAll().associate { it.path to it.addedAt }
            } catch (e: Exception) {
                emptyMap()
            }

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

            // Bulk upsert + mark stale + delete stale.
            directoryDao.syncReplaceAll(entities, now)

            // 3. Update sync state.
            val newState = SyncStateEntity(
                id = 0,
                lastSyncAt = now,
                listadoEtag = meta.etag,
                listadoLastModified = meta.lastModified,
                listadoSize = meta.contentLength,
                totalDirectories = entities.size,
                newDirectoriesLastSync = entities.count { it.addedAt == now },
                removedDirectoriesLastSync = 0,
                lastError = null
            )
            syncStateDao.upsert(newState)

            Result.success()
        } catch (e: Exception) {
            // Save the error in sync_state.
            val now = System.currentTimeMillis()
            val cur = syncStateDao.get() ?: SyncStateEntity(
                id = 0,
                lastSyncAt = now,
                listadoEtag = null,
                listadoLastModified = null,
                listadoSize = 0,
                totalDirectories = 0,
                newDirectoriesLastSync = 0,
                removedDirectoriesLastSync = 0,
                lastError = null
            )
            syncStateDao.upsert(cur.copy(lastSyncAt = now, lastError = e.message ?: e.toString()))
            // Retry with backoff.
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "listado_sync"

        fun schedule(context: Context, intervalHours: Long = AppConstants.DEFAULT_SYNC_INTERVAL_HOURS) {
            val request = PeriodicWorkRequestBuilder<ListadoSyncWorker>(
                intervalHours, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInitialDelay(30, TimeUnit.SECONDS)  // let app finish initializing
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        suspend fun runNow(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<ListadoSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
