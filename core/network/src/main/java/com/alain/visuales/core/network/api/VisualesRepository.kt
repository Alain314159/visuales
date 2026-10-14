package com.alain.visuales.core.network.api

import com.alain.visuales.core.network.model.RemoteDirectory
import com.alain.visuales.core.network.model.RemoteEntry
import com.alain.visuales.core.network.model.ResourceMetadata
import com.alain.visuales.core.network.parser.ListadoHtmlParser
import com.alain.visuales.core.network.scraper.NginxyListingScraper
import okhttp3.Response
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level network repository for the Visuales server.
 *
 * Wraps the Retrofit API and the parsers/scraper to expose clean methods that
 * return domain-ready DTOs. The Room layer never sees HTML directly.
 */
@Singleton
class VisualesRepository @Inject constructor(
    private val api: VisualesApi,
    private val retrofit: Retrofit,
    private val listadoParser: ListadoHtmlParser,
    private val scraper: NginxyListingScraper
) {

    /**
     * Fetch and parse the full directory tree from listado.html.
     *
     * @param baseUrl server base URL (default https://visuales.uclv.cu).
     * @return list of all directories on the server.
     */
    @Throws(IOException::class)
    suspend fun fetchDirectoryTree(baseUrl: String): List<RemoteDirectory> {
        val url = baseUrl.trimEnd('/') + "/listado.html"
        val html = api.getListadoHtml(url)
        return listadoParser.parse(html, baseUrl)
    }

    /**
     * Fetch the HTML of a single folder's listing and parse it.
     *
     * @param folderUrl absolute URL of the folder (must end with "/").
     * @return list of entries (files + sub-directories) found in the folder.
     */
    @Throws(IOException::class)
    suspend fun fetchDirectoryContents(folderUrl: String): List<RemoteEntry> {
        val html = api.getDirectoryListing(folderUrl)
        return scraper.parse(html, folderUrl)
    }

    /**
     * Issue a HEAD request to learn a resource's metadata (size, dates, etag).
     * Used by the sync worker to skip re-downloading listado.html if unchanged.
     */
    @Throws(IOException::class)
    suspend fun headResource(url: String): ResourceMetadata {
        val response: Response = api.headFile(url)
        val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
        val lastModified = response.header("Last-Modified")?.let { parseHttpDate(it) }
        val etag = response.header("ETag")
        val code = response.code
        response.close()
        return ResourceMetadata(code, contentLength, lastModified, etag)
    }

    private fun parseHttpDate(httpDate: String): Long? {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMM d HH:mm:ss yyyy"
        )
        for (pattern in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH).apply {
                    timeZone = java.util.TimeZone.getTimeZone("GMT")
                }
                val date = sdf.parse(httpDate) ?: continue
                return date.time
            } catch (e: Exception) {
                // Try next format.
            }
        }
        return null
    }
}
