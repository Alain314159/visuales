package com.alain.visuales.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatalogSortModeTest {

    @Test
    fun `all sort modes have distinct query params`() {
        val params = CatalogSortMode.entries.map { it.queryParam }
        assertThat(params).containsNoDuplicates()
    }

    @Test
    fun `all sort modes have distinct display labels`() {
        val labels = CatalogSortMode.entries.map { it.displayLabel }
        assertThat(labels).containsNoDuplicates()
    }

    @Test
    fun `NAME_ASC produces correct Apache autoindex param`() {
        assertThat(CatalogSortMode.NAME_ASC.queryParam).isEqualTo("C=N;O=A")
    }

    @Test
    fun `NAME_DESC produces correct Apache autoindex param`() {
        assertThat(CatalogSortMode.NAME_DESC.queryParam).isEqualTo("C=N;O=D")
    }

    @Test
    fun `DATE_ASC produces correct Apache autoindex param`() {
        assertThat(CatalogSortMode.DATE_ASC.queryParam).isEqualTo("C=M;O=A")
    }

    @Test
    fun `DATE_DESC produces correct Apache autoindex param`() {
        assertThat(CatalogSortMode.DATE_DESC.queryParam).isEqualTo("C=M;O=D")
    }

    @Test
    fun `SIZE_ASC produces correct Apache autoindex param`() {
        assertThat(CatalogSortMode.SIZE_ASC.queryParam).isEqualTo("C=S;O=A")
    }

    @Test
    fun `SIZE_DESC produces correct Apache autoindex param`() {
        assertThat(CatalogSortMode.SIZE_DESC.queryParam).isEqualTo("C=S;O=D")
    }

    @Test
    fun `DEFAULT is NAME_ASC`() {
        assertThat(CatalogSortMode.DEFAULT).isEqualTo(CatalogSortMode.NAME_ASC)
    }

    @Test
    fun `there are exactly 6 sort modes`() {
        assertThat(CatalogSortMode.entries).hasSize(6)
    }
}

class CommunityLinksTest {

    @Test
    fun `COMMUNITY_LINKS is non-empty`() {
        assertThat(COMMUNITY_LINKS).isNotEmpty()
    }

    @Test
    fun `COMMUNITY_LINKS contains telegram links`() {
        val telegramLinks = COMMUNITY_LINKS.filter { it.category == "Telegram" }
        assertThat(telegramLinks).isNotEmpty()
        telegramLinks.forEach { link ->
            assertThat(link.url).contains("t.me/")
        }
    }

    @Test
    fun `COMMUNITY_LINKS contains UCLV server links`() {
        val serverLinks = COMMUNITY_LINKS.filter { it.category == "Servidores UCLV" }
        assertThat(serverLinks).isNotEmpty()
        serverLinks.forEach { link ->
            assertThat(link.url).contains("uclv")
        }
    }

    @Test
    fun `all community links have non-empty id, title, url`() {
        COMMUNITY_LINKS.forEach { link ->
            assertThat(link.id).isNotEmpty()
            assertThat(link.title).isNotEmpty()
            assertThat(link.url).isNotEmpty()
        }
    }

    @Test
    fun `all community link URLs start with http`() {
        COMMUNITY_LINKS.forEach { link ->
            assertThat(link.url).startsWith("http")
        }
    }

    @Test
    fun `all community link IDs are unique`() {
        val ids = COMMUNITY_LINKS.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }
}

class AppConstantsTest {

    @Test
    fun `DEFAULT_BASE_URL points to visuales uclv cu`() {
        assertThat(AppConstants.DEFAULT_BASE_URL).isEqualTo("https://visuales.uclv.cu")
    }

    @Test
    fun `LISTADO_HTML_PATH is slash listado dot html`() {
        assertThat(AppConstants.LISTADO_HTML_PATH).isEqualTo("/listado.html")
    }

    @Test
    fun `USER_AGENT contains VisualesAndroid identifier`() {
        assertThat(AppConstants.USER_AGENT).contains("VisualesAndroid")
    }

    @Test
    fun `DEFAULT_CONNECTIONS_PER_FILE is 8`() {
        assertThat(AppConstants.DEFAULT_CONNECTIONS_PER_FILE).isEqualTo(8)
    }

    @Test
    fun `DEFAULT_MAX_CONCURRENT_DOWNLOADS is 2`() {
        assertThat(AppConstants.DEFAULT_MAX_CONCURRENT_DOWNLOADS).isEqualTo(2)
    }

    @Test
    fun `DEFAULT_SYNC_INTERVAL_HOURS is 24`() {
        assertThat(AppConstants.DEFAULT_SYNC_INTERVAL_HOURS).isEqualTo(24)
    }

    @Test
    fun `DEFAULT_DIRECTORY_TTL_MS is 24 hours`() {
        assertThat(AppConstants.DEFAULT_DIRECTORY_TTL_MS).isEqualTo(24L * 60 * 60 * 1000)
    }

    @Test
    fun `DOWNLOAD_FOLDER_NAME is Visuales`() {
        assertThat(AppConstants.DOWNLOAD_FOLDER_NAME).isEqualTo("Visuales")
    }

    @Test
    fun `GITHUB_REPO points to Alain314159 visuales`() {
        assertThat(AppConstants.GITHUB_REPO).isEqualTo("Alain314159/visuales")
    }
}
