package com.alain.visuales.core.data.repository

import com.alain.visuales.core.network.api.VisualesRepository
import com.alain.visuales.core.network.subtitles.SubDbApi
import com.alain.visuales.core.network.subtitles.SubdivxScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that resolves subtitles for a downloaded video file using a cascade:
 *
 *  1. Sibling .srt file on the server (visuales.uclv.cu)
 *     e.g. /Peliculas/Cubanas/Viva Cuba.mp4 → /Peliculas/Cubanas/Viva Cuba.srt
 *
 *  2. SubDB by file hash (no credentials, no API key)
 *
 *  3. Subdivx scraping (no credentials, Spanish-only)
 *
 * The first layer that returns a non-empty .srt file wins. The result is
 * extracted if zipped and written to the destination next to the video.
 */
@Singleton
class SubtitleRepository @Inject constructor(
    private val subDb: SubDbApi,
    private val subdivx: SubdivxScraper,
    private val visualesRepo: VisualesRepository,
    private val client: OkHttpClient
) {

    /**
     * Try to fetch a Spanish subtitle for the given video file.
     *
     * @param videoFile the downloaded video on local disk (used for SubDB hash).
     * @param videoName display name without extension (used for Subdivx search).
     * @param subtitleUrl optional URL of a sibling .srt on the visuales server (if known).
     * @param destinationDir directory to write the .srt file to.
     * @return the resolved .srt file, or null if all layers failed.
     */
    suspend fun fetchSpanishSubtitle(
        videoFile: File,
        videoName: String,
        subtitleUrl: String?,
        destinationDir: File
    ): File? = withContext(Dispatchers.IO) {
        val baseName = videoName.substringBeforeLast('.')
        val targetSrt = File(destinationDir, "$baseName.srt")

        // Layer 1: sibling .srt on the visuales server
        if (subtitleUrl != null) {
            if (downloadFromVisuales(subtitleUrl, targetSrt)) {
                return@withContext targetSrt
            }
        }

        // Layer 2: SubDB by hash
        val hash = subDb.computeHash(videoFile)
        if (hash != null) {
            val langs = subDb.searchLanguages(hash)
            if ("es" in langs) {
                if (subDb.downloadSubtitle(hash, "es", targetSrt) && targetSrt.length() > 0) {
                    return@withContext targetSrt
                }
            }
            // Try Latin American Spanish variant
            if ("es-la" in langs || "es-ar" in langs || "es-mx" in langs) {
                val lang = listOf("es-la", "es-ar", "es-mx").first { it in langs }
                if (subDb.downloadSubtitle(hash, lang, targetSrt) && targetSrt.length() > 0) {
                    return@withContext targetSrt
                }
            }
            // Fallback to English if no Spanish at all
            if ("en" in langs) {
                if (subDb.downloadSubtitle(hash, "en", targetSrt) && targetSrt.length() > 0) {
                    return@withContext targetSrt
                }
            }
        }

        // Layer 3: Subdivx scraping (Spanish-only)
        val query = sanitizeSearchQuery(videoName)
        if (query.isNotBlank()) {
            val results = subdivx.search(query)
            for (result in results.take(3)) {
                val zipped = File(destinationDir, "${baseName}_subdivx.zip")
                if (subdivx.download(result, zipped) && zipped.length() > 0) {
                    if (extractSubtitleFromZip(zipped, targetSrt)) {
                        zipped.delete()
                        return@withContext targetSrt
                    }
                    zipped.delete()
                }
            }
        }

        null
    }

    /**
     * Download a .srt from the visuales server.
     * Returns true if the file was successfully written.
     */
    private suspend fun downloadFromVisuales(url: String, target: File): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body ?: return false
                val contentType = body.contentType()?.toString().orEmpty()
                target.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                // Validate: must be a text file (not a 404 HTML page)
                target.length() > 0 && !contentType.contains("text/html") &&
                    !target.readText().startsWith("<html", ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extract the first .srt file from a .zip archive (subdivx often serves .zip).
     */
    private fun extractSubtitleFromZip(zipFile: File, targetSrt: File): Boolean {
        return try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".srt", ignoreCase = true)) {
                        targetSrt.parentFile?.mkdirs()
                        FileOutputStream(targetSrt).use { fos ->
                            zis.copyTo(fos)
                        }
                        return targetSrt.length() > 0
                    }
                    entry = zis.nextEntry
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clean up the video name for Subdivx search.
     * - Remove year prefix like "2005_"
     * - Remove quality markers like "[1080p]", "(BluRay)", ".HEVC", ".x265"
     * - Replace underscores with spaces
     */
    private fun sanitizeSearchQuery(name: String): String {
        var q = name
            .substringBeforeLast('.')  // remove extension
            .replace('_', ' ')
            .replace('.', ' ')
            .replaceRegex(Regex("\\[.*?]"), "")  // [1080p]
            .replaceRegex(Regex("\\(.*?\\)"), "")  // (BluRay)
            .replaceRegex(Regex("\\b(1080p|720p|480p|2160p|4K|HEVC|x265|x264|H264|H265|BluRay|WEBRip|WEB-DL|DVDRip|BDRip|MeGusta|EZTVx|to)\\b", RegexOption.IGNORE_CASE), "")
            .replaceRegex(Regex("^\\d{4}\\s*-\\s*"), "")  // "2024 - "
            .replaceRegex(Regex("^\\d{4}_"), "")  // "2005_"
            .trim()
        // Limit to first 60 chars (better search match)
        if (q.length > 60) q = q.substring(0, 60).trim()
        return q
    }

    private fun String.replaceRegex(regex: Regex, replacement: String): String =
        regex.replace(this, replacement)
}
