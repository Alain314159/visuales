package com.alain.visuales.core.common

/**
 * Native Apache autoindex sort modes supported by visuales.uclv.cu.
 *
 * Apache's mod_autoindex accepts ?C=X;O=Y query params:
 *   C = column to sort by (N=name, M=modified, S=size, D=description)
 *   O = order (A=ascending, D=descending)
 *
 * These are appended to directory URLs to get sorted listings, e.g.:
 *   https://visuales.uclv.cu/Recientes/?C=M;O=D  (most recent first)
 */
enum class CatalogSortMode(val queryParam: String, val displayLabel: String) {
    NAME_ASC("C=N;O=A", "Nombre (A-Z)"),
    NAME_DESC("C=N;O=D", "Nombre (Z-A)"),
    DATE_ASC("C=M;O=A", "Más antiguo primero"),
    DATE_DESC("C=M;O=D", "Más reciente primero"),
    SIZE_ASC("C=S;O=A", "Más pequeño primero"),
    SIZE_DESC("C=S;O=D", "Más grande primero");

    companion object {
        val DEFAULT = NAME_ASC
    }
}

/**
 * Community links discovered in /REDES_SOCIALES.txt on the visuales server.
 *
 * These are social/community resources the admin left for users. We expose them
 * in Settings → Comunidad so users can join Telegram channels, follow the admin,
 * read blogs, etc.
 */
data class CommunityLink(
    val id: String,
    val title: String,
    val url: String,
    val category: String
)

val COMMUNITY_LINKS = listOf(
    CommunityLink(
        id = "telegram_cineytv",
        title = "Canal de Telegram — Cine y TV",
        url = "https://t.me/cineytv",
        category = "Telegram"
    ),
    CommunityLink(
        id = "telegram_matrix",
        title = "Grupo de Telegram — TheMatrixN",
        url = "https://t.me/TheMatrixN",
        category = "Telegram"
    ),
    CommunityLink(
        id = "telegram_privado",
        title = "Canal privado de descargas",
        url = "https://t.me/+gdvZPyip8IcwNGQ5",
        category = "Telegram"
    ),
    CommunityLink(
        id = "twitter",
        title = "Twitter del admin @BGretter1",
        url = "https://twitter.com/BGretter1",
        category = "Redes sociales"
    ),
    CommunityLink(
        id = "blog_losmasnew",
        title = "Blog LosMásNew",
        url = "https://losmasnew.wordpress.com/",
        category = "Blogs"
    ),
    CommunityLink(
        id = "blog_letterboxd",
        title = "Letterboxd — Pasión por el Cine",
        url = "https://letterboxd.com/pasionxelcine/",
        category = "Blogs"
    ),
    CommunityLink(
        id = "podcast_ever",
        title = "Podcast Arte Ver (ivoox)",
        url = "https://www.ivoox.com/podcast-arte-ver_sq_f1924829_1.html",
        category = "Podcast"
    ),
    CommunityLink(
        id = "web_visuales",
        title = "Visuales UCLV (web oficial)",
        url = "https://visuales.uclv.cu",
        category = "Servidores UCLV"
    ),
    CommunityLink(
        id = "web_emby",
        title = "Emby UCLV",
        url = "https://emby.uclv.cu",
        category = "Servidores UCLV"
    ),
    CommunityLink(
        id = "web_uclv",
        title = "Universidad Central Marta Abreu de Las Villas",
        url = "https://www.uclv.edu.cu",
        category = "Servidores UCLV"
    )
)
