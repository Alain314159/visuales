package com.alain.visuales.core.network.subtitles

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SubDb subtitle API client.
 *
 * SubDb is a free, centralized subtitle database that requires NO API key and
 * NO authentication. Subtitles are looked up by a hash of the video file:
 *
 *   MD5 of (first 64KB of file ++ last 64KB of file)
 *
 * API:
 *   GET http://api.thesubdb.com/?action=search&hash={hash}     -> "es,en,pt" (CSV)
 *   GET http://api.thesubdb.com/?action=download&hash={hash}&language=es -> .srt body
 *
 * Requirements:
 *   - User-Agent must be in format "SubDB/1.0 (AppName/Version; URL)"
 *   - File must be at least 128KB (otherwise hash is just MD5 of the whole file)
 *
 * URL: http://thesubdb.com/api/
 */
@Singleton
class SubDbApi @Inject constructor(
    private val client: OkHttpClient
) {
    private val baseUrl = "http://api.thesubdb.com/"
    private val userAgent = "SubDB/1.0 (Visuales/2.0; https://github.com/Alain314159/visuales)"

    /**
     * Compute the SubDB hash of a video file.
     *
     * @return lowercase hex MD5 string, or null if file is too small.
     */
    fun computeHash(file: File): String? {
        if (!file.exists()) return null
        val size = file.length()
        if (size < 128 * 1024) {
            // Too small: SubDB doesn't have a hash for files < 128KB
            return null
        }

        val first64k = ByteArray(64 * 1024)
        val last64k = ByteArray(64 * 1024)

        RandomAccessFile(file, "r").use { raf ->
            raf.seek(0)
            raf.readFully(first64k)
            raf.seek(size - 64 * 1024)
            raf.readFully(last64k)
        }

        val md5 = MessageDigest.getInstance("MD5")
        md5.update(first64k)
        md5.update(last64k)
        return md5.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Search available subtitle languages for a given video hash.
     *
     * @param hash MD5 hash from [computeHash].
     * @return set of language codes ("es", "en", "pt", "fr", etc.) or empty set on error.
     */
    fun searchLanguages(hash: String): Set<String> {
        val request = Request.Builder()
            .url("${baseUrl}?action=search&hash=$hash")
            .header("User-Agent", userAgent)
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptySet()
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) emptySet()
                else body.split(",").map { it.trim().lowercase() }.toSet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Download a subtitle file for the given hash in the given language.
     *
     * @param hash MD5 hash from [computeHash].
     * @param language ISO 639-1 code ("es", "en", "pt", "fr", ...).
     * @param outputFile destination .srt file (will be created or overwritten).
     * @return true if the download succeeded and the file is non-empty.
     */
    fun downloadSubtitle(hash: String, language: String, outputFile: File): Boolean {
        val request = Request.Builder()
            .url("${baseUrl}?action=download&hash=$hash&language=$language")
            .header("User-Agent", userAgent)
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body ?: return false
                outputFile.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                outputFile.exists() && outputFile.length() > 0
            }
        } catch (e: Exception) {
            false
        }
    }
}
