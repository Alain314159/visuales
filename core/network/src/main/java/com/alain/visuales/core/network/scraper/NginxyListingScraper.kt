package com.alain.visuales.core.network.scraper

import com.alain.visuales.core.common.EntryType
import com.alain.visuales.core.network.model.RemoteEntry
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Scraper for the Nginxy-themed directory listing served by visuales.uclv.cu
 * for any given folder.
 *
 * The HTML structure produced by Nginxy + nginx-fancyindex looks roughly like:
 *
 *   <table>
 *     <tr class="parent">
 *       <td></td>
 *       <td><a href="../">Parent directory</a></td>
 *       <td></td><td></td>
 *     </tr>
 *     <tr>
 *       <td><img src="/nginxy/images/icons/folder.png"></td>
 *       <td><a href="SomeFolder/">SomeFolder</a></td>
 *       <td>14-Oct-2026 11:24</td>
 *       <td>-</td>
 *     </tr>
 *     <tr>
 *       <td><img src="/nginxy/images/icons/mp4.png"></td>
 *       <td><a href="Viva Cuba (2005).mp4">Viva Cuba (2005).mp4</a></td>
 *       <td>14-Oct-2026 11:24</td>
 *       <td>1.4G</td>
 *     </tr>
 *   </table>
 *
 * Note: the config.js of the theme uses jQuery at runtime to inject icon
 * <img> tags. When we scrape via HTTP (no JS execution), the icon `<img>` is
 * NOT present in the raw HTML — only the `<a href>` is. Therefore we classify
 * entries by their filename extension, NOT by their icon.
 *
 * In some Nginxy configurations the icon is rendered server-side. To be robust,
 * we try both: prefer icon-based classification, fall back to extension.
 */
class NginxyListingScraper {

    /**
     * Parse the HTML of a directory listing.
     *
     * @param html raw HTML returned by the server for a folder URL.
     * @param folderUrl the absolute URL of the folder being scraped.
     *   Used to resolve relative links.
     */
    fun parse(html: String, folderUrl: String): List<RemoteEntry> {
        val doc: Document = Jsoup.parse(html, folderUrl)
        val baseUrl = folderUrl.trimEnd('/')

        // Strategy 1: Nginxy renders a table with rows.
        val tableRows = doc.select("table tr")
        if (tableRows.isNotEmpty()) {
            return tableRows.mapNotNull { row -> parseTableRow(row, baseUrl, folderUrl) }
        }

        // Strategy 2: fallback — collect all <a> tags directly.
        return doc.select("a[href]").mapNotNull { a ->
            parseAnchor(a, baseUrl, folderUrl)
        }
    }

    private fun parseTableRow(row: Element, baseUrl: String, folderUrl: String): RemoteEntry? {
        val anchor = row.selectFirst("a[href]") ?: return null
        return parseAnchor(anchor, baseUrl, folderUrl, rowEl = row)
    }

    private fun parseAnchor(
        anchor: Element,
        baseUrl: String,
        folderUrl: String,
        rowEl: Element? = null
    ): RemoteEntry? {
        val rawHref = anchor.absUrl("href").ifEmpty { anchor.attr("href") }
        if (rawHref.isEmpty()) return null

        // Skip the theme assets (images, css, js, header, footer).
        if (rawHref.contains("/nginxy/")) return null

        // Skip "Parent directory" link.
        val text = anchor.text().trim()
        if (text.equals("Parent directory", ignoreCase = true) || rawHref.endsWith("../")) return null

        // Resolve absolute URL.
        val absoluteUrl = resolveUrl(rawHref, baseUrl)

        // Compute the path relative to the server root.
        val relativePath = extractPath(absoluteUrl)

        // Decode the path for human-readable storage.
        val decodedPath = decodePath(relativePath)
        val decodedName = text.ifEmpty {
            decodedPath.trimEnd('/').substringAfterLast('/')
        }

        val isDirectory = absoluteUrl.endsWith("/") || decodedName.isEmpty() && relativePath.endsWith("/")

        // Try to find size + date from sibling cells.
        var size: Long = 0
        var modifiedAt: Long? = null

        if (rowEl != null) {
            val cells = rowEl.select("td")
            // Nginxy table layout: [icon, name, date, size]
            for (cell in cells.drop(1)) {  // skip first cell (icon)
                val cellText = cell.text().trim()
                if (cellText.isEmpty()) continue

                // Try date first (matches "14-Oct-2026 11:24" or similar).
                val parsedDate = parseDate(cellText)
                if (parsedDate != null) {
                    modifiedAt = parsedDate
                    continue
                }

                // Try size (matches "1.4G", "850M", "120K", "-" etc.).
                val parsedSize = parseSize(cellText)
                if (parsedSize != null) {
                    size = parsedSize
                    continue
                }
            }
        }

        val type = if (isDirectory) {
            EntryType.DIRECTORY.name
        } else {
            EntryType.fromFilename(decodedName).name
        }

        return RemoteEntry(
            name = decodedName,
            url = absoluteUrl,
            path = decodedPath,
            isDirectory = isDirectory,
            size = size,
            modifiedAt = modifiedAt,
            type = type
        )
    }

    /**
     * Resolve a possibly-relative href to an absolute URL.
     */
    private fun resolveUrl(href: String, baseUrl: String): String {
        return try {
            URL(URL(baseUrl), href).toString()
        } catch (e: Exception) {
            href
        }
    }

    /**
     * Extract the path component of a URL (no query, no fragment).
     */
    private fun extractPath(url: String): String {
        return try {
            URL(url).path
        } catch (e: Exception) {
            url
        }
    }

    /**
     * URL-decode a path while preserving slashes.
     */
    private fun decodePath(path: String): String {
        return path.split("/").joinToString("/") { segment ->
            URLDecoder.decode(segment, "UTF-8")
        }
    }

    /**
     * Parse a date string in Nginxy's default format.
     * Default nginx fancyindex time_format is "%e-%b-%Y %H:%M"
     * Example: "14-Oct-2026 11:24"
     *
     * Some installations use "%d-%b-%Y %H:%M" (zero-padded): "14-Oct-2026 11:24"
     * Some use ISO: "2026-10-14 11:24"
     */
    private fun parseDate(s: String): Long? {
        if (s.isBlank()) return null

        // Try common Nginxy date formats.
        val formats = listOf(
            "d-MMM-yyyy HH:mm",
            "dd-MMM-yyyy HH:mm",
            "yyyy-MM-dd HH:mm",
            "EEE, dd MMM yyyy HH:mm:ss zzz"
        )

        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(s) ?: continue
                return date.time
            } catch (e: Exception) {
                // Try next format.
            }
        }
        return null
    }

    /**
     * Parse a Nginxy size string into bytes.
     * Examples: "1.4G", "850M", "120K", "5B", "-", "".
     *
     * Returns null if the string is not a size (could be a date or other text).
     */
    private fun parseSize(s: String): Long? {
        if (s.isBlank() || s == "-") return 0
        val match = Regex("^[\\d.]+\\s*([KMGTP]?i?B?|B)?$").find(s) ?: return null
        val valueStr = match.value.trim().replace(Regex("[^\\d.]"), "")
        if (valueStr.isEmpty()) return null
        val value = valueStr.toDoubleOrNull() ?: return null

        val multiplier = when {
            s.endsWith("KiB") || s.endsWith("K") -> 1024.0
            s.endsWith("MiB") || s.endsWith("M") -> 1024.0 * 1024
            s.endsWith("GiB") || s.endsWith("G") -> 1024.0 * 1024 * 1024
            s.endsWith("TiB") || s.endsWith("T") -> 1024.0 * 1024 * 1024 * 1024
            s.endsWith("PiB") || s.endsWith("P") -> 1024.0 * 1024 * 1024 * 1024 * 1024
            s.endsWith("B") -> 1.0
            else -> 1.0
        }
        return (value * multiplier).toLong()
    }
}
