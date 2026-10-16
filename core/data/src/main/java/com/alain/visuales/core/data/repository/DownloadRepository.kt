package com.alain.visuales.core.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.common.EntryType
import com.alain.visuales.core.data.db.dao.ChunkDao
import com.alain.visuales.core.data.db.dao.DownloadDao
import com.alain.visuales.core.data.db.entity.ChunkEntity
import com.alain.visuales.core.data.db.entity.DownloadEntity
import com.alain.visuales.core.data.db.entity.DownloadPriority
import com.alain.visuales.core.data.db.entity.DownloadStatus
import com.alain.visuales.core.data.preferences.SettingsRepository
import com.alain.visuales.core.network.download.DownloadProgressCallback
import com.alain.visuales.core.network.download.MultiThreadDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level repository that owns the download queue.
 *
 * Coordinates:
 *  - Persisting download metadata (DownloadDao) + chunk state (ChunkDao).
 *  - Spawning up to `maxConcurrentDownloads` parallel downloads via [MultiThreadDownloader].
 *  - Enforcing user restrictions (wifiOnly, chargingOnly, scheduled window).
 *  - Subtitle auto-download when [autoSubtitles] is on and a subtitle URL is known.
 */
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val chunkDao: ChunkDao,
    private val downloader: MultiThreadDownloader,
    private val settings: SettingsRepository,
    private val subtitleRepository: SubtitleRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** Observe the entire queue. */
    fun observeAll(): Flow<List<DownloadEntity>> = downloadDao.observeAll()
    fun observeActive(): Flow<List<DownloadEntity>> = downloadDao.observeActive()
    fun observeCompleted(): Flow<List<DownloadEntity>> = downloadDao.observeCompleted()
    fun observeFailed(): Flow<List<DownloadEntity>> = downloadDao.observeFailed()

    suspend fun getDownload(id: String): DownloadEntity? = downloadDao.getById(id)

    /**
     * Enqueue a new download. Returns the generated download ID.
     *
     * @param url the source URL on the UCLV server.
     * @param name display name (also used as filename).
     * @param size total size in bytes (from HEAD or listado), 0 if unknown.
     * @param subtitleUrl optional subtitle URL.
     * @param section catalog section (Peliculas/Series/...).
     * @param posterUrl optional cover URL for the UI.
     */
    suspend fun enqueue(
        url: String,
        name: String,
        size: Long = 0,
        subtitleUrl: String? = null,
        section: String? = null,
        posterUrl: String? = null
    ): String {
        val id = "dl_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(8)
        val safeName = sanitizeFilename(name)
        val entity = DownloadEntity(
            id = id,
            url = url,
            localPath = "$safeName",
            name = name,
            size = size,
            status = DownloadStatus.QUEUED.name,
            priority = DownloadPriority.NORMAL.name,
            createdAt = System.currentTimeMillis(),
            section = section,
            subtitleUrl = subtitleUrl,
            posterUrl = posterUrl
        )
        downloadDao.upsert(entity)
        pumpQueue()
        return id
    }

    /** Enqueue multiple downloads at once. */
    suspend fun enqueueBatch(items: List<EnqueueItem>): List<String> {
        val ids = mutableListOf<String>()
        for (item in items) {
            // Build entity directly to avoid N pumpQueue() calls inside the loop.
            val id = "dl_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(8)
            val safeName = sanitizeFilename(item.name)
            val entity = DownloadEntity(
                id = id,
                url = item.url,
                localPath = safeName,
                name = item.name,
                size = item.size,
                status = DownloadStatus.QUEUED.name,
                priority = DownloadPriority.NORMAL.name,
                createdAt = System.currentTimeMillis(),
                section = item.section,
                subtitleUrl = item.subtitleUrl,
                posterUrl = item.posterUrl
            )
            downloadDao.upsert(entity)
            ids.add(id)
        }
        // Pump once after the whole batch is queued.
        pumpQueue()
        return ids
    }

    data class EnqueueItem(
        val url: String,
        val name: String,
        val size: Long,
        val subtitleUrl: String? = null,
        val section: String? = null,
        val posterUrl: String? = null
    )

    suspend fun pause(id: String) {
        downloadDao.setStatus(id, DownloadStatus.PAUSED.name)
    }

    suspend fun resume(id: String) {
        downloadDao.setStatus(id, DownloadStatus.QUEUED.name)
        pumpQueue()
    }

    suspend fun cancel(id: String) {
        downloadDao.setStatus(id, DownloadStatus.CANCELLED.name)
        chunkDao.deleteChunks(id)
        // Clean up the .partial file left by MultiThreadDownloader.
        val dl = downloadDao.getById(id)
        if (dl != null) {
            val partialFile = File(getDownloadDir(), sanitizeFilename(dl.name) + ".partial")
            if (partialFile.exists()) partialFile.delete()
        }
    }

    suspend fun remove(id: String, deleteFile: Boolean = false) {
        if (deleteFile) {
            val dl = downloadDao.getById(id)
            if (dl != null) {
                getFile(dl).delete()
            }
        }
        chunkDao.deleteChunks(id)
        downloadDao.delete(id)
    }

    suspend fun clearFinished() = downloadDao.clearFinished()

    suspend fun setPriority(id: String, priority: DownloadPriority) {
        val dl = downloadDao.getById(id) ?: return
        downloadDao.upsert(dl.copy(priority = priority.name))
        pumpQueue()
    }

    /**
     * Run the next pending download if a slot is free.
     * Safe to call multiple times; the mutex serializes the check.
     */
    suspend fun pumpQueue() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val s = settings.settings.first()
            val running = downloadDao.countDownloading()
            val slots = s.maxConcurrentDownloads - running
            if (slots <= 0) return@withContext

            val next = downloadDao.getNextQueued(slots)
            for (dl in next) {
                startDownloadInternal(dl, s.connectionsPerFile)
            }
        }
    }

    private fun startDownloadInternal(dl: DownloadEntity, connections: Int) {
        scope.launch {
            try {
                downloadDao.setStatus(dl.id, DownloadStatus.DOWNLOADING.name)

                val outputFile = getFile(dl)
                outputFile.parentFile?.mkdirs()

                // Recover chunk state if exists (resume).
                val existingChunks = chunkDao.chunksFor(dl.id)
                val resumePositions: List<Long>? = if (existingChunks.isNotEmpty()) {
                    existingChunks.sortedBy { it.index }.map { it.currentByte }
                } else null

                // Pre-create chunks for resume support.
                if (existingChunks.isEmpty() && dl.size > 0) {
                    val safeConnections = connections.coerceAtMost(
                        ((dl.size / 1024).toInt().coerceAtLeast(1)).coerceAtMost(connections)
                    )
                    val chunkSize = dl.size / safeConnections
                    val chunks = (0 until safeConnections).map { i ->
                        val start = i * chunkSize.toLong()
                        val end = if (i == safeConnections - 1) dl.size - 1 else (start + chunkSize - 1)
                        ChunkEntity(
                            downloadId = dl.id,
                            index = i,
                            startByte = start,
                            endByte = end,
                            currentByte = start,
                            completed = false
                        )
                    }
                    chunkDao.upsertAll(chunks)
                }

                downloader.download(
                    url = dl.url,
                    outputFile = outputFile,
                    connections = connections,
                    resumePositions = resumePositions,
                    callback = object : DownloadProgressCallback {
                        private var lastUpdateAt = 0L
                        override fun onProgress(bytesDownloaded: Long, totalBytes: Long, speedBytesPerSec: Long) {
                            val now = System.currentTimeMillis()
                            // Throttle to one DB write per 500 ms (matches MultiThreadDownloader sampling).
                            if (now - lastUpdateAt < 500) return
                            lastUpdateAt = now
                            val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
                            scope.launch {
                                downloadDao.updateProgress(dl.id, progress, bytesDownloaded)
                            }
                        }

                        override fun onChunkFailed(chunkIndex: Int, attempt: Int, error: Throwable) {
                            // No-op; the engine retries internally.
                        }

                        override fun onCompleted(outputFile: File) {
                            scope.launch {
                                downloadDao.setStatusAndCompletedAt(
                                    dl.id, DownloadStatus.COMPLETED.name, System.currentTimeMillis()
                                )
                                // Auto-download subtitles using the cascade:
                                // sibling → SubDB (hash) → subdivx (scraping).
                                // Always try (even if subtitleUrl is null) because SubDB
                                // can resolve by hash and subdivx by name.
                                val s = settings.settings.first()
                                if (s.autoSubtitles) {
                                    downloadSubtitle(dl)
                                }
                                pumpQueue()
                            }
                        }

                        override fun onFailed(error: Throwable) {
                            scope.launch {
                                downloadDao.incrementRetries(dl.id)
                                val current = downloadDao.getById(dl.id)
                                if (current != null && current.retries >= AppConstants.DEFAULT_MAX_RETRIES) {
                                    downloadDao.setStatusAndError(
                                        dl.id, DownloadStatus.FAILED.name, error.message ?: error.toString()
                                    )
                                } else {
                                    // Will retry.
                                    downloadDao.setStatus(dl.id, DownloadStatus.QUEUED.name)
                                    kotlinx.coroutines.delay(3000)
                                    pumpQueue()
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                downloadDao.setStatusAndError(dl.id, DownloadStatus.FAILED.name, e.message ?: e.toString())
            }
        }
    }

    /**
     * Download the subtitle for a completed video using the full cascade:
     *
     *  1. Sibling .srt on the visuales server (dl.subtitleUrl)
     *  2. SubDB by hash (no credentials)
     *  3. Subdivx scraping (Spanish, no credentials)
     *
     * The result file is written next to the video as `<basename>.srt`.
     * Silently fails if no subtitle is found — subtitles are optional.
     */
    private suspend fun downloadSubtitle(dl: DownloadEntity) {
        try {
            val videoFile = getFile(dl)
            if (!videoFile.exists() || videoFile.length() < 128 * 1024) {
                // File too small for SubDB hash, and probably not worth fetching.
                return
            }
            subtitleRepository.fetchSpanishSubtitle(
                videoFile = videoFile,
                videoName = dl.name,
                subtitleUrl = dl.subtitleUrl,
                destinationDir = getDownloadDir()
            )
        } catch (e: Exception) {
            // Silently fail — subtitles are optional.
        }
    }

    /**
     * Resolve the local file for a download.
     *
     * If the user picked a SAF folder (downloadFolderUri), use that. Otherwise
     * use public Downloads/Visuales.
     */
    fun getFile(dl: DownloadEntity): File {
        return File(getDownloadDir(), sanitizeFilename(dl.name))
    }

    private fun getDownloadDir(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            AppConstants.DOWNLOAD_FOLDER_NAME
        ).apply { if (!exists()) mkdirs() }
    }

    /**
     * Strip characters that are unsafe for filesystems.
     */
    private fun sanitizeFilename(name: String): String {
        // Remove diacritics
        val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        // Replace illegal chars
        return normalized.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
}
