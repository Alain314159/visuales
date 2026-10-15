package com.alain.visuales.core.network.subtitles

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subdivx subtitle scraper.
 *
 * Subdivx.com is a Spanish subtitle database that does NOT require an API key.
 * We scrape the search results page and follow the download URL.
 *
 * Page format:
 *   GET https://www.subdivx.com/index.php?buscar={query}&accion=5
 *
 * Each result has:
 *   - Title (movie name + quality + uploader)
 *   - Download link (a boton descargar with onclick="descargar(7,{id})
 *     which redirects to /down2.php?num={id})
 *   - Subtitle details (language, fps, etc.)
 *
 * Because subdivx uses a POST endpoint for the actual download, we follow:
 *   1. Search HTML page
 *   2. Find each "descargar" link and the associated subtitle ID
 *   3. POST to /down2.php with the subtitle id
 *
 * URL: https://www.subdivx.com/
 */
@Singleton
class SubdivxScraper @Inject constructor(
    private val client: OkHttpClient
) {
    private val baseUrl = "https://www.subdivx.com"

    /**
     * Search subdivx for subtitles matching the query.
     *
     * @param query typically the movie/show name without extension.
     * @return list of [SubdivxResult] with id, title, and download url.
     */
    fun search(query: String): List<SubdivxResult> {
        val searchUrl = "$baseUrl/index.php?buscar=${java.net.URLEncoder.encode(query, "UTF-8")}&accion=5"

        val request = Request.Builder()
            .url(searchUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept-Language", "es-ES,es;q=0.9")
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val html = resp.body?.string().orEmpty()
                parseSearchResults(html)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Download a subtitle file.
     *
     * @param result the [SubdivxResult] from [search].
     * @param outputFile destination .srt/.zip file (subdivx often serves .zip).
     * @return true if the download succeeded.
     */
    fun download(result: SubdivxResult, outputFile: java.io.File): Boolean {
        // subdivx serves downloads via /down2.php with POST and the subtitle id
        val downloadUrl = "$baseUrl/down2.php"
        val formBody = FormBody.Builder()
            .add("num", result.id)
            .build()

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Referer", "$baseUrl/index.php")
            .post(formBody)
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

    private fun parseSearchResults(html: String): List<SubdivxResult> {
        val doc: Document = Jsoup.parse(html, baseUrl)
        val results = mutableListOf<SubdivxResult>()

        // subdivx uses <div class="titulo_menu"> for each result entry
        val entries = doc.select("div.titulo_menu, div.menu_detalle_buscador")
        for (entry in entries) {
            // Find the download link
            val downloadLink = entry.selectFirst("a[href*=down2.php], a[onclick*=descargar]")
            val title = entry.selectFirst("a.titulo_menu, a.link_menu, .titulo_menu a")?.text()
                ?: entry.text().trim().take(200)

            // Extract the subtitle id from href like "down2.php?num=123456"
            val href = downloadLink?.attr("href").orEmpty()
            val idMatch = Regex("num=(\\d+)").find(href)
                ?: Regex("descargar\\((\\d+),(\\d+)").find(downloadLink?.attr("onclick").orEmpty())
                ?.let { it.groupValues[2] }
                ?: continue

            results.add(
                SubdivxResult(
                    id = idMatch,
                    title = title,
                    downloadUrl = "$baseUrl/down2.php?num=$idMatch"
                )
            )
        }

        return results.distinctBy { it.id }
    }
}

data class SubdivxResult(
    val id: String,
    val title: String,
    val downloadUrl: String
)
