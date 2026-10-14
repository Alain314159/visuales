package com.alain.visuales.core.common

/**
 * App-wide constants. Centralized so feature modules can stay decoupled.
 */
object AppConstants {
    /** Default UCLV Visuales base URL. */
    const val DEFAULT_BASE_URL = "https://visuales.uclv.cu"

    /** Relative path on the server that returns the full directory tree as HTML. */
    const val LISTADO_HTML_PATH = "/listado.html"

    /** Relative path of the Nginxy theme assets on the server. */
    const val NGINXY_ASSETS_PATH = "/nginxy"

    /** GitHub repo for self-update checks. */
    const val GITHUB_REPO = "Alain314159/visuales"

    /** User-Agent for all HTTP requests (server admins can identify us). */
    const val USER_AGENT = "VisualesAndroid/2.0 (+https://github.com/Alain314159/visuales)"

    /** Default cache TTL for a directory listing scraped from the server (24h). */
    const val DEFAULT_DIRECTORY_TTL_MS = 24L * 60 * 60 * 1000

    /** Default number of parallel HTTP connections per downloaded file (multi-thread). */
    const val DEFAULT_CONNECTIONS_PER_FILE = 8

    /** Default max concurrent downloads. */
    const val DEFAULT_MAX_CONCURRENT_DOWNLOADS = 2

    /** Default max retries on transient network failures. */
    const val DEFAULT_MAX_RETRIES = 3

    /** Default sync interval for the catalog (24h). */
    const val DEFAULT_SYNC_INTERVAL_HOURS = 24L

    /** Download folder name (under public Downloads). */
    const val DOWNLOAD_FOLDER_NAME = "Visuales"
}

/**
 * Top-level sections of the UCLV catalog (extracted from listado.html).
 * Used for the Home screen carousels and for filtering.
 */
object CatalogSections {
    const val CURSOS = "Cursos"
    const val DOCUMENTALES = "Documentales"
    const val FESTIVALES = "Festivales y Premios de cine"
    const val INFANTILES = "Infantiles"
    const val LA_MOCHILA = "LaMochila"
    const val LIBROS = "Libros"
    const val MEGA_LIBROS = "MegaLibros"
    const val MP3S = "MP3s"
    const val PA_QUE_TE_DUQES = "PaQueTeDuqes"
    const val PELICULAS = "Peliculas"
    const val PETICIONES = "Peticiones"
    const val PROGRAMACION_COMPETITIVA = "ProgramacionCompetitiva"
    const val REALITY_SHOWS = "Reality Shows"
    const val RECIENTES = "Recientes"
    const val SERIES = "Series"

    /** Sections that should be hidden from the user (server internals). */
    val HIDDEN = setOf("nginxy")

    /** Default visible sections on Home (in display order). */
    val FEATURED = listOf(
        RECIENTES,
        PELICULAS,
        SERIES,
        DOCUMENTALES,
        CURSOS,
        INFANTILES,
        LA_MOCHILA
    )
}
