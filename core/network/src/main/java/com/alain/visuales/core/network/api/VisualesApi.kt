package com.alain.visuales.core.network.api

import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Low-level HTTP API for the UCLV Visuales server.
 *
 * All URLs are passed as absolute because we're navigating the tree (URLs come
 * from the listado.html file and per-folder listings).
 */
interface VisualesApi {

    /** Fetch the listado.html file containing the full directory tree. */
    @GET
    suspend fun getListadoHtml(@Url url: String): String

    /** Fetch a per-folder directory listing (raw HTML). */
    @GET
    suspend fun getDirectoryListing(@Url url: String): String

    /**
     * Issue a HEAD request to a file to learn its size, last modified and ETag.
     * Returns the raw OkHttp response so the caller can read all headers.
     */
    @HEAD
    suspend fun headFile(@Url url: String): Response

    /**
     * Download a byte range of a file (used by the multi-threaded downloader).
     * The `Range` header is set by the caller.
     */
    @Streaming
    @GET
    suspend fun downloadRange(
        @Url url: String,
        @Header("Range") range: String
    ): ResponseBody
}
