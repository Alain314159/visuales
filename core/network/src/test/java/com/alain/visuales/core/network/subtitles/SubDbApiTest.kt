package com.alain.visuales.core.network.subtitles

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SubDbApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SubDbApi
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        // SubDbApi uses a hardcoded baseUrl; we override it via reflection for testing.
        api = SubDbApi(OkHttpClient.Builder().build())
        // Replace baseUrl with mock server URL using reflection.
        val field = SubDbApi::class.java.getDeclaredField("baseUrl")
        field.isAccessible = true
        field.set(api, server.url("/").toString().trimEnd('/'))
        client = OkHttpClient.Builder().build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `searchLanguages returns set of language codes on 200`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("es,en,pt")
        )
        val langs = api.searchLanguages("abc123hash")
        assertThat(langs).containsExactly("es", "en", "pt")
    }

    @Test
    fun `searchLanguages returns empty set on 404`() {
        server.enqueue(MockResponse().setResponseCode(404))
        val langs = api.searchLanguages("abc123hash")
        assertThat(langs).isEmpty()
    }

    @Test
    fun `searchLanguages returns empty set on server error`() {
        server.enqueue(MockResponse().setResponseCode(500))
        val langs = api.searchLanguages("abc123hash")
        assertThat(langs).isEmpty()
    }

    @Test
    fun `searchLanguages includes User-Agent header`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("es")
        )
        api.searchLanguages("abc123hash")
        val request: RecordedRequest = server.takeRequest()
        val userAgent = request.getHeader("User-Agent")
        assertThat(userAgent).isNotNull()
        assertThat(userAgent!!).contains("SubDB/1.0")
    }

    @Test
    fun `searchLanguages sends correct query params`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("es")
        )
        api.searchLanguages("myhash123")
        val request: RecordedRequest = server.takeRequest()
        val path = request.path ?: ""
        assertThat(path).contains("action=search")
        assertThat(path).contains("hash=myhash123")
    }

    @Test
    fun `downloadSubtitle writes file on 200`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("1\n00:00:01,000 --> 00:00:02,000\nHello world\n")
        )
        val tempFile = Files.createTempFile("subtitle", ".srt").toFile()
        tempFile.deleteOnExit()
        val success = api.downloadSubtitle("myhash123", "es", tempFile)
        assertThat(success).isTrue()
        assertThat(tempFile.exists()).isTrue()
        assertThat(tempFile.length()).isGreaterThan(0)
        val content = tempFile.readText()
        assertThat(content).contains("Hello world")
    }

    @Test
    fun `downloadSubtitle returns false on 404`() {
        server.enqueue(MockResponse().setResponseCode(404))
        val tempFile = Files.createTempFile("subtitle", ".srt").toFile()
        tempFile.deleteOnExit()
        val success = api.downloadSubtitle("myhash123", "es", tempFile)
        assertThat(success).isFalse()
    }

    @Test
    fun `downloadSubtitle sends correct query params`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("subtitle content")
        )
        val tempFile = Files.createTempFile("subtitle", ".srt").toFile()
        tempFile.deleteOnExit()
        api.downloadSubtitle("hash456", "es", tempFile)
        val request: RecordedRequest = server.takeRequest()
        val path = request.path ?: ""
        assertThat(path).contains("action=download")
        assertThat(path).contains("hash=hash456")
        assertThat(path).contains("language=es")
    }

    @Test
    fun `computeHash returns null for non-existent file`() {
        val result = api.computeHash(File("/nonexistent/file.mp4"))
        assertThat(result).isNull()
    }

    @Test
    fun `computeHash returns null for file smaller than 128KB`() {
        val tempFile = Files.createTempFile("small", ".mp4").toFile()
        tempFile.deleteOnExit()
        // Write 1000 bytes — smaller than 128KB threshold.
        Files.write(tempFile.toPath(), ByteArray(1000) { it.toByte() })
        val result = api.computeHash(tempFile)
        assertThat(result).isNull()
    }

    @Test
    fun `computeHash returns 32-char hex string for valid file`() {
        val tempFile = Files.createTempFile("video", ".mp4").toFile()
        tempFile.deleteOnExit()
        // Write 256KB of data (enough for first+last 64KB).
        val data = ByteArray(256 * 1024) { (it % 256).toByte() }
        Files.write(tempFile.toPath(), data)
        val result = api.computeHash(tempFile)
        assertThat(result).isNotNull()
        assertThat(result!!.length).isEqualTo(32) // MD5 hex
        // Should only contain hex chars.
        assertThat(result.matches(Regex("^[a-f0-9]{32}$"))).isTrue()
    }
}
