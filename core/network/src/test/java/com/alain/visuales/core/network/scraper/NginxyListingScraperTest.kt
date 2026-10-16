package com.alain.visuales.core.network.scraper

import com.alain.visuales.core.common.EntryType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NginxyListingScraperTest {

    private val scraper = NginxyListingScraper()
    private val folderUrl = "https://visuales.uclv.cu/Peliculas/Cubanas/"

    @Test
    fun `parse returns empty list for empty html`() {
        val result = scraper.parse("", folderUrl)
        assertThat(result).isEmpty()
    }

    @Test
    fun `parse returns empty list for html without links`() {
        val html = "<html><body><h1>No files</h1></body></html>"
        val result = scraper.parse(html, folderUrl)
        assertThat(result).isEmpty()
    }

    @Test
    fun `parse extracts single video file entry`() {
        val html = """
            <html><body>
            <table>
              <tr><td></td><td><a href="Viva%20Cuba.mp4">Viva Cuba.mp4</a></td><td>2026-08-06 04:17</td><td>1.4G</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, folderUrl)
        assertThat(result).hasSize(1)
        val entry = result[0]
        assertThat(entry.name).isEqualTo("Viva Cuba.mp4")
        assertThat(entry.url).isEqualTo("https://visuales.uclv.cu/Peliculas/Cubanas/Viva%20Cuba.mp4")
        assertThat(entry.isDirectory).isFalse()
        assertThat(entry.type).isEqualTo(EntryType.VIDEO.name)
        assertThat(entry.size).isEqualTo(1_503_238_553L) // 1.4 * 1024^3
    }

    @Test
    fun `parse extracts directory entries`() {
        val html = """
            <html><body>
            <table>
              <tr><td></td><td><a href="Subfolder/">Subfolder/</a></td><td>2026-08-06 04:17</td><td>-</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, folderUrl)
        assertThat(result).hasSize(1)
        val entry = result[0]
        assertThat(entry.name).isEqualTo("Subfolder")
        assertThat(entry.isDirectory).isTrue()
        assertThat(entry.type).isEqualTo(EntryType.DIRECTORY.name)
    }

    @Test
    fun `parse skips parent directory link`() {
        val html = """
            <html><body>
            <table>
              <tr class="parent"><td></td><td><a href="../">Parent directory</a></td><td></td><td></td></tr>
              <tr><td></td><td><a href="Movie.mp4">Movie.mp4</a></td><td>2026-08-06 04:17</td><td>1G</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, folderUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Movie.mp4")
    }

    @Test
    fun `parse skips nginxy theme asset links`() {
        val html = """
            <html><body>
            <a href="/.nginxy/css/style.css">style.css</a>
            <a href="/.nginxy/js/config.js">config.js</a>
            <table>
              <tr><td></td><td><a href="Movie.mp4">Movie.mp4</a></td><td>2026-08-06 04:17</td><td>1G</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, folderUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Movie.mp4")
    }

    @Test
    fun `parse classifies entry by extension when icon is missing`() {
        val html = """
            <html><body>
            <table>
              <tr><td></td><td><a href="poster.jpg">poster.jpg</a></td><td>2026-08-06 04:17</td><td>120K</td></tr>
              <tr><td></td><td><a href="sub.srt">sub.srt</a></td><td>2026-08-06 04:17</td><td>5K</td></tr>
              <tr><td></td><td><a href="song.mp3">song.mp3</a></td><td>2026-08-06 04:17</td><td>5M</td></tr>
              <tr><td></td><td><a href="book.pdf">book.pdf</a></td><td>2026-08-06 04:17</td><td>2M</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, folderUrl)
        assertThat(result).hasSize(4)
        assertThat(result[0].type).isEqualTo(EntryType.IMAGE.name)
        assertThat(result[1].type).isEqualTo(EntryType.SUBTITLE.name)
        assertThat(result[2].type).isEqualTo(EntryType.AUDIO.name)
        assertThat(result[3].type).isEqualTo(EntryType.DOCUMENT.name)
    }

    @Test
    fun `parse classifies mkv as video even though Nginxy theme lacks mkv icon`() {
        val html = """
            <html><body>
            <table>
              <tr><td></td><td><a href="Movie.mkv">Movie.mkv</a></td><td>2026-08-06 04:17</td><td>2G</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, folderUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(EntryType.VIDEO.name)
    }

    @Test
    fun `parse handles Apache autoindex HTML format`() {
        // Apache autoindex uses HTML 3.2 with <table> + <tr><td valign=...
        // Real visuales.uclv.cu home uses this format.
        val html = """
            <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
            <html><head><title>Index of /</title></head><body>
            <table>
              <tr><th><img src="/icons/blank.gif" alt="[ICO]"></th><th><a href="?C=N;O=D">Name</a></th><th><a href="?C=M;O=A">Last modified</a></th><th><a href="?C=S;O=A">Size</a></th><th><a href="?C=D;O=A">Description</a></th></tr>
              <tr><th colspan="5"><hr></th></tr>
              <tr><td valign="top"><img src="/icons/folder.gif" alt="[DIR]"></td><td><a href="Documentales/">Documentales/</a></td><td align="right">2026-08-06 04:17  </td><td align="right">  - </td><td>&nbsp;</td></tr>
              <tr><td valign="top"><img src="/icons/folder.gif" alt="[DIR]"></td><td><a href="Peliculas/">Peliculas/</a></td><td align="right">2026-08-06 04:17  </td><td align="right">  - </td><td>&nbsp;</td></tr>
              <tr><td valign="top"><img src="/icons/movie.gif" alt="[VID]"></td><td><a href="trailer.mp4">trailer.mp4</a></td><td align="right">2026-10-09 05:00  </td><td align="right">120M</td><td>&nbsp;</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, "https://visuales.uclv.cu/")
        // Should include Documentales, Peliculas, trailer.mp4 (but skip ?C=... links).
        assertThat(result).hasSize(3)
        val dirs = result.filter { it.isDirectory }
        val files = result.filter { !it.isDirectory }
        assertThat(dirs).hasSize(2)
        assertThat(files).hasSize(1)
        assertThat(files[0].name).isEqualTo("trailer.mp4")
        assertThat(files[0].type).isEqualTo(EntryType.VIDEO.name)
    }

    @Test
    fun `parse parses Apache autoindex dates in ISO format`() {
        val html = """
            <html><body>
            <table>
              <tr><td></td><td><a href="Movie.mp4">Movie.mp4</a></td><td align="right">2026-08-06 04:17  </td><td align="right">1G</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, folderUrl)
        assertThat(result).hasSize(1)
        val entry = result[0]
        assertThat(entry.modifiedAt).isNotNull()
        assertThat(entry.modifiedAt!!).isGreaterThan(0L)
    }

    @Test
    fun `parse parses sizes with various units`() {
        val html = """
            <html><body>
            <table>
              <tr><td></td><td><a href="a.mp4">a.mp4</a></td><td>2026-08-06 04:17</td><td>500M</td></tr>
              <tr><td></td><td><a href="b.mp4">b.mp4</a></td><td>2026-08-06 04:17</td><td>1.4G</td></tr>
              <tr><td></td><td><a href="c.mp4">c.mp4</a></td><td>2026-08-06 04:17</td><td>800K</td></tr>
              <tr><td></td><td><a href="d.mp4">d.mp4</a></td><td>2026-08-06 04:17</td><td>-</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val result = scraper.parse(html, folderUrl)
        assertThat(result).hasSize(4)
        assertThat(result[0].size).isEqualTo(524_288_000L) // 500 MB
        assertThat(result[1].size).isEqualTo(1_503_238_553L) // 1.4 GB
        assertThat(result[2].size).isEqualTo(819_200L) // 800 KB
        assertThat(result[3].size).isEqualTo(0L) // -
    }
}
