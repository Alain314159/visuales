package com.alain.visuales.core.network.download

import com.alain.visuales.core.network.model.ResourceMetadata
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Progress callback for a multi-threaded download. Called on background threads.
 */
interface DownloadProgressCallback {
    fun onProgress(bytesDownloaded: Long, totalBytes: Long, speedBytesPerSec: Long)
    fun onChunkFailed(chunkIndex: Int, attempt: Int, error: Throwable)
    fun onCompleted(outputFile: File)
    fun onFailed(error: Throwable)
}

/**
 * State of a single chunk being downloaded.
 */
data class ChunkState(
    val index: Int,
    val startByte: Long,
    val endByte: Long,
    val currentByte: AtomicLong = AtomicLong(startByte),
    @Volatile var completed: Boolean = false
) {
    val size: Long get() = endByte - startByte + 1
    fun downloaded(): Long = currentByte.get() - startByte
}

/**
 * Multi-threaded HTTP downloader using Range requests.
 *
 * Splits the file into N chunks (default 8) and downloads each in parallel with
 * its own OkHttp connection. Each chunk writes to its own offset in a single
 * RandomAccessFile using seek(). Retries with exponential backoff per chunk.
 *
 * Falls back to single-thread when the server doesn't support ranges.
 */
@Singleton
class MultiThreadDownloader @Inject constructor(
    private val client: OkHttpClient
) {

    /**
     * Synchronously download a file with N parallel connections.
     * Blocks the calling thread until completion or failure.
     *
     * @param url URL to download.
     * @param outputFile target file (will be created if not exists).
     * @param connections number of parallel connections (1..32). Default 8.
     * @param resumePositions if non-null, the starting byte of each chunk (for resume).
     * @param callback receives progress and completion events.
     */
    suspend fun download(
        url: String,
        outputFile: File,
        connections: Int = 8,
        resumePositions: List<Long>? = null,
        callback: DownloadProgressCallback
    ) = withContext(Dispatchers.IO) {
        val effectiveConnections = connections.coerceIn(1, 32)

        // Stage 1: HEAD request.
        val metadata = head(url)
        if (metadata.statusCode !in 200..299) {
            callback.onFailed(IOException("HEAD returned HTTP ${metadata.statusCode}"))
            return@withContext
        }

        val totalSize = metadata.contentLength
        if (totalSize <= 0) {
            // Unknown size — single-thread fallback.
            downloadSingleThread(url, outputFile, callback)
            return@withContext
        }

        // Stage 2: prepare chunks.
        val chunkSize: Long = totalSize / effectiveConnections
        val chunks = (0 until effectiveConnections).map { i ->
            val start: Long = i.toLong() * chunkSize
            val end: Long = if (i == effectiveConnections - 1) totalSize - 1L else (start + chunkSize - 1L)
            val resumeFrom: Long = resumePositions?.getOrNull(i)?.coerceIn(start, end + 1L) ?: start
            ChunkState(i, start, end, AtomicLong(resumeFrom))
        }

        // Stage 3: pre-allocate the file.
        val partialFile = File(outputFile.absolutePath + ".partial")
        if (partialFile.exists() && partialFile.length() != totalSize) {
            partialFile.delete()
        }
        if (!partialFile.exists()) {
            partialFile.parentFile?.mkdirs()
            RandomAccessFile(partialFile, "rw").use { it.setLength(totalSize) }
        }

        // Stage 4: download all chunks in parallel.
        val totalDownloaded = AtomicLong(chunks.sumOf { it.downloaded() })

        try {
            coroutineScope {
                val jobs = chunks.map { chunk ->
                    async(Dispatchers.IO) {
                        downloadChunk(url, partialFile, chunk, callback, totalDownloaded, totalSize)
                    }
                }
                jobs.awaitAll()
            }
            // Rename .partial to final.
            if (outputFile.exists()) outputFile.delete()
            if (!partialFile.renameTo(outputFile)) {
                throw IOException("Failed to rename partial file to ${outputFile.absolutePath}")
            }
            callback.onCompleted(outputFile)
        } catch (e: Exception) {
            callback.onFailed(e)
        }
    }

    private suspend fun downloadChunk(
        url: String,
        file: File,
        chunk: ChunkState,
        callback: DownloadProgressCallback,
        totalDownloaded: AtomicLong,
        totalSize: Long
    ) {
        val maxAttempts = 4
        var attempt = 0
        var lastError: Throwable? = null

        while (attempt < maxAttempts) {
            attempt++
            try {
                val range = "bytes=${chunk.currentByte.get()}-${chunk.endByte}"
                val request = Request.Builder()
                    .url(url)
                    .header("Range", range)
                    .build()

                val response: Response = client.newCall(request).execute()
                response.use { resp ->
                    if (resp.code != 206 && resp.code != 200) {
                        throw IOException("HTTP ${resp.code} for chunk ${chunk.index}")
                    }
                    val body = resp.body ?: throw IOException("Empty body for chunk ${chunk.index}")
                    val input = body.byteStream()
                    RandomAccessFile(file, "rw").use { writer ->
                        writer.seek(chunk.currentByte.get())
                        val buffer = ByteArray(64 * 1024)
                        var lastSpeedSampleAt = System.currentTimeMillis()
                        var lastSpeedSampleBytes = totalDownloaded.get()
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            writer.write(buffer, 0, read)
                            val newPos = chunk.currentByte.addAndGet(read.toLong())
                            totalDownloaded.addAndGet(read.toLong())
                            val now = System.currentTimeMillis()
                            if (now - lastSpeedSampleAt > 500) {
                                val elapsed = now - lastSpeedSampleAt
                                val bytes = totalDownloaded.get() - lastSpeedSampleBytes
                                val speed = if (elapsed > 0) bytes * 1000 / elapsed else 0
                                callback.onProgress(totalDownloaded.get(), totalSize, speed)
                                lastSpeedSampleAt = now
                                lastSpeedSampleBytes = totalDownloaded.get()
                            }
                            if (newPos >= chunk.endByte + 1) {
                                chunk.completed = true
                                return
                            }
                        }
                    }
                }
                return
            } catch (e: Exception) {
                lastError = e
                callback.onChunkFailed(chunk.index, attempt, e)
                if (attempt < maxAttempts) {
                    delay(1000L shl (attempt - 1))
                }
            }
        }
        throw IOException("Chunk ${chunk.index} failed after $maxAttempts attempts", lastError)
    }

    /**
     * HEAD request to learn Content-Length / Accept-Ranges / ETag / Last-Modified.
     */
    suspend fun head(url: String): ResourceMetadata = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).head().build()
        client.newCall(request).execute().use { resp ->
            val contentLength = resp.header("Content-Length")?.toLongOrNull() ?: -1
            val lastModified = resp.header("Last-Modified")?.let { parseHttpDate(it) }
            val etag = resp.header("ETag")
            ResourceMetadata(resp.code, contentLength, lastModified, etag)
        }
    }

    private fun downloadSingleThread(
        url: String,
        outputFile: File,
        callback: DownloadProgressCallback
    ) {
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body ?: throw IOException("Empty body")
                outputFile.parentFile?.mkdirs()
                outputFile.outputStream().use { out ->
                    val input = body.byteStream()
                    val buffer = ByteArray(64 * 1024)
                    var totalDownloaded = 0L
                    val totalSize = body.contentLength().let { if (it > 0) it else -1 }
                    var lastSampleAt = System.currentTimeMillis()
                    var lastSampleBytes = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        totalDownloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastSampleAt > 500) {
                            val elapsed = now - lastSampleAt
                            val bytes = totalDownloaded - lastSampleBytes
                            val speed = if (elapsed > 0) bytes * 1000 / elapsed else 0
                            callback.onProgress(totalDownloaded, totalSize, speed)
                            lastSampleAt = now
                            lastSampleBytes = totalDownloaded
                        }
                    }
                }
                callback.onCompleted(outputFile)
            }
        } catch (e: Exception) {
            callback.onFailed(e)
        }
    }

    private fun parseHttpDate(httpDate: String): Long? {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEEE, dd-MMM-yy HH:mm:ss zzz"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("GMT")
                }
                return sdf.parse(httpDate)?.time ?: continue
            } catch (_: Exception) { /* try next */ }
        }
        return null
    }
}
