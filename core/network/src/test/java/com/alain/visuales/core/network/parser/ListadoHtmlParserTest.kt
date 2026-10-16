package com.alain.visuales.core.network.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ListadoHtmlParserTest {

    private val parser = ListadoHtmlParser()
    private val baseUrl = "https://visuales.uclv.cu"

    @Test
    fun `parse returns empty list for empty html`() {
        val result = parser.parse("", baseUrl)
        assertThat(result).isEmpty()
    }

    @Test
    fun `parse returns empty list for html without links`() {
        val html = "<html><body><h1>No links here</h1></body></html>"
        val result = parser.parse(html, baseUrl)
        assertThat(result).isEmpty()
    }

    @Test
    fun `parse extracts single directory link`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/Cursos/">Cursos</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Cursos")
        assertThat(result[0].path).isEqualTo("/Cursos/")
        assertThat(result[0].url).isEqualTo("https://visuales.uclv.cu/Cursos/")
    }

    @Test
    fun `parse normalizes double slash in URLs`() {
        // The visuales server returns URLs with double slash after domain.
        // Use the same protocol as baseUrl (https) to avoid protocol mismatch filtering.
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu//Cursos/">Cursos</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
        // The parser should extract the directory regardless of double slash.
        assertThat(result[0].name).isEqualTo("Cursos")
        assertThat(result[0].path).isEqualTo("/Cursos/")
    }

    @Test
    fun `parse extracts multiple nested directories`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/Cursos/">Cursos</a>
            <a href="https://visuales.uclv.cu/Cursos/AWS/">AWS</a>
            <a href="https://visuales.uclv.cu/Cursos/AWS/Intro/">Intro</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(3)

        val aws = result.find { it.name == "AWS" }!!
        assertThat(aws.path).isEqualTo("/Cursos/AWS/")
        assertThat(aws.parentPath).isEqualTo("/Cursos")
        assertThat(aws.depth).isEqualTo(2)
        assertThat(aws.section).isEqualTo("Cursos")
        assertThat(aws.subcategory).isEqualTo("AWS")
    }

    @Test
    fun `parse extracts root path section correctly`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/Peliculas/">Peliculas</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].section).isEqualTo("Peliculas")
        assertThat(result[0].parentPath).isEqualTo("/")
        assertThat(result[0].depth).isEqualTo(1)
    }

    @Test
    fun `parse URL-decodes spaces in paths`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/Peliculas/Cubanas/1938_Romance%20del%20Palmar/">1938_Romance del Palmar</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].path).isEqualTo("/Peliculas/Cubanas/1938_Romance del Palmar/")
        assertThat(result[0].name).isEqualTo("1938_Romance del Palmar")
    }

    @Test
    fun `parse ignores links outside the base URL`() {
        val html = """
            <html><body>
            <a href="https://example.com/foo/">External</a>
            <a href="https://visuales.uclv.cu/Inside/">Inside</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Inside")
    }

    @Test
    fun `parse ignores non-directory links (no trailing slash)`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/listado.html">listado.html</a>
            <a href="https://visuales.uclv.cu/master.zip">master.zip</a>
            <a href="https://visuales.uclv.cu/Cursos/">Cursos</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Cursos")
    }

    @Test
    fun `parse skips the root URL itself`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/">Root</a>
            <a href="https://visuales.uclv.cu/Cursos/">Cursos</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        // Root link is skipped because path becomes "/" which we ignore.
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Cursos")
    }

    @Test
    fun `parse deduplicates by path`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/Cursos/">Cursos</a>
            <a href="https://visuales.uclv.cu/Cursos/">Cursos</a>
            <a href="http://visuales.uclv.cu//Cursos/">Cursos</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
    }

    @Test
    fun `parse handles MHTML wrapper by stripping it`() {
        val html = """From: <Saved by Blink>
Snapshot-Content-Location: https://visuales.uclv.cu/listado.html
Subject: Directory Tree
Date: Wed, 14 Oct 2026 10:52:49 GMT
MIME-Version: 1.0
Content-Type: multipart/related;
        type="text/html";
        boundary="----MultipartBoundary--test----"


------MultipartBoundary--test----
Content-Type: text/html
Content-Location: https://visuales.uclv.cu/listado.html

<html><body>
<a href="https://visuales.uclv.cu/Cursos/">Cursos</a>
</body></html>
------MultipartBoundary--test------
"""
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Cursos")
    }

    @Test
    fun `parse extracts subcategory for depth-2 paths`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/Peliculas/Cubanas/">Cubanas</a>
            <a href="https://visuales.uclv.cu/Peliculas/Extranjeras/">Extranjeras</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(2)
        assertThat(result[0].section).isEqualTo("Peliculas")
        assertThat(result[0].subcategory).isEqualTo("Cubanas")
        assertThat(result[1].section).isEqualTo("Peliculas")
        assertThat(result[1].subcategory).isEqualTo("Extranjeras")
    }

    @Test
    fun `parse handles deep nesting correctly`() {
        val html = """
            <html><body>
            <a href="https://visuales.uclv.cu/Series/NETFLIX/Squid%20Game/Squid.Game.S02/">Squid.Game.S02</a>
            </body></html>
        """.trimIndent()
        val result = parser.parse(html, baseUrl)
        assertThat(result).hasSize(1)
        assertThat(result[0].path).isEqualTo("/Series/NETFLIX/Squid Game/Squid.Game.S02/")
        assertThat(result[0].name).isEqualTo("Squid.Game.S02")
        assertThat(result[0].depth).isEqualTo(4)
        assertThat(result[0].section).isEqualTo("Series")
        assertThat(result[0].subcategory).isEqualTo("NETFLIX")
    }
}
