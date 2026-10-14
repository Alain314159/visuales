package com.alain.visuales.core.network.parser

import com.alain.visuales.core.network.model.RemoteDirectory
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.net.URLDecoder

/**
 * Parser for the UCLV Visuales "listado.html" file.
 *
 * The server exposes this file at https://visuales.uclv.cu/listado.html and it
 * contains the output of `tree` rendered as HTML with one `<a href>` per
 * directory (no files, only directories). Each link's href is the absolute URL
 * of the directory on the server.
 *
 * When the file is downloaded from a browser and saved as MHTML, it has an
 * MHTML wrapper that needs to be stripped before parsing. This parser handles
 * both the raw HTML and the MHTML-wrapped form.
 *
 * Quirks found in the actual file:
 *  - URLs come with double slash: `http://visuales.uclv.cu//Cursos/` (server bug).
 *  - URLs are URL-encoded (%20 for spaces).
 *  - The HTML has 30000+ `<a>` tags, all ending in `/` (directories only).
 *  - The first `<a>` is the root URL itself (e.g. http://visuales.uclv.cu/).
 */
class ListadoHtmlParser {

    /**
     * Parse the file content. Auto-detects MHTML vs raw HTML.
     *
     * @param baseUrl the base URL of the server, e.g. "https://visuales.uclv.cu".
     *   Used to filter out external links and to compute relative paths.
     */
    fun parse(content: String, baseUrl: String): List<RemoteDirectory> {
        val html = stripMhtmlWrapper(content)
        val doc: Document = Jsoup.parse(html, baseUrl)
        val links = doc.select("a[href]")

        // Normalize baseUrl to the form we'll match against.
        val normalizedBase = baseUrl.trimEnd('/')

        return links.mapNotNull { a ->
            val rawHref = a.absUrl("href").ifEmpty { a.attr("href") }
            if (rawHref.isEmpty()) return@mapNotNull null

            // Normalize: collapse double slashes after the host.
            val url = normalizeUrl(rawHref) ?: return@mapNotNull null

            // Skip if not under our base URL.
            if (!url.startsWith(normalizedBase)) return@mapNotNull null

            // Skip if doesn't end with "/" — we only care about directories.
            if (!url.endsWith("/")) return@mapNotNull null

            // Skip the root URL itself (we don't store it as a "directory").
            val path = extractRelativePath(url, normalizedBase)
            if (path.isEmpty() || path == "/") return@mapNotNull null

            // Decode the path segments.
            val decodedPath = decodePath(path)
            val decodedName = decodedPath.trimEnd('/').substringAfterLast('/')
            val decodedParent = decodedPath.trimEnd('/').substringBeforeLast('/', "").ifEmpty { "/" }

            val segments = decodedPath.trim('/').split("/").filter { it.isNotEmpty() }
            val section = segments.firstOrNull() ?: ""
            val subcategory = segments.getOrNull(1)?.takeIf { it.isNotEmpty() }

            RemoteDirectory(
                url = url,
                path = decodedPath.let { if (it.endsWith("/")) it else "$it/" },
                name = decodedName,
                parentPath = decodedParent,
                depth = segments.size,
                section = section,
                subcategory = subcategory
            )
        }.distinctBy { it.path }
    }

    /**
     * If the content is an MHTML file, extract only the text/html part.
     * Otherwise return the input unchanged.
     */
    private fun stripMhtmlWrapper(content: String): String {
        if (!content.startsWith("From:") && !content.contains("MIME-Version")) {
            return content
        }
        // MHTML structure:
        //   From: <Saved by Blink>
        //   Snapshot-Content-Location: ...
        //   Subject: ...
        //   MIME-Version: 1.0
        //   Content-Type: multipart/related; boundary="..."
        //
        //   --boundary
        //   Content-Type: text/html
        //   ...
        //   <html>...</html>
        //   --boundary
        //   Content-Type: text/css
        //   ...
        //   --boundary--

        val boundaryMatch = Regex("boundary=\"?([^\"\\r\\n]+)\"?").find(content)
        val boundary = boundaryMatch?.groupValues?.get(1) ?: return content

        val parts = content.split("--$boundary")
        for (part in parts) {
            // Find the headers/body separator (blank line).
            val separatorIdx = part.indexOf("\r\n\r\n").let {
                if (it >= 0) it else part.indexOf("\n\n")
            }
            if (separatorIdx < 0) continue

            val headers = part.substring(0, separatorIdx)
            val body = part.substring(separatorIdx).trimStart('\r', '\n')

            if (headers.contains("Content-Type: text/html", ignoreCase = true)) {
                return body
            }
        }
        return content
    }

    /**
     * Normalize a URL: collapse duplicate slashes in the path.
     * "http://visuales.uclv.cu//Cursos/" → "http://visuales.uclv.cu/Cursos/"
     */
    private fun normalizeUrl(raw: String): String? {
        return try {
            val u = URL(raw)
            val cleanedPath = u.path.replace(Regex("/+"), "/")
            val rebuilt = URL(u.protocol, u.host, u.port, cleanedPath).toString()
            // Preserve the query and ref (rare in this server, but be safe).
            if (u.query != null) "$rebuilt?${u.query}"
            else if (u.ref != null) "$rebuilt#${u.ref}"
            else rebuilt
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract the path relative to the base URL.
     * "https://visuales.uclv.cu/Cursos/12%20Factor%20App/" → "/Cursos/12%20Factor%20App/"
     */
    private fun extractRelativePath(url: String, baseUrl: String): String {
        return url.removePrefix(baseUrl).let { if (it.isEmpty()) "/" else it }
    }

    /**
     * URL-decode a path while preserving slashes.
     * "/Cursos/12%20Factor%20App/" → "/Cursos/12 Factor App/"
     */
    private fun decodePath(path: String): String {
        return path.split("/").joinToString("/") { segment ->
            URLDecoder.decode(segment, "UTF-8")
        }
    }
}
