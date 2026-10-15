package com.alain.visuales.core.network.api

import com.alain.visuales.core.network.model.EmbyAuthResponse
import com.alain.visuales.core.network.model.EmbyItem
import com.alain.visuales.core.network.model.EmbyItemsResponse
import com.alain.visuales.core.network.model.EmbyViewsResponse
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Emby 4.9.3.0 server at `https://emby.uclv.cu/emby/`.
 *
 * **Important**: Emby Web expects client identification (`X-Emby-*`) to be
 * passed as **URL query parameters**, not HTTP headers. We comply by using
 * `@Query` annotations for each of them on every authenticated request. The
 * single exception is `POST /Users/AuthenticateByName`, which accepts an
 * `X-Emby-Authorization` **header** (in the
 * `MediaBrowser Client="...", Device="...", DeviceId="...", Version="..."`
 * format) because the token has not been issued yet at that point.
 *
 * All endpoints return JSON. The DTOs in
 * `com.alain.visuales.core.network.model` are decoded with the official
 * kotlinx-serialization Retrofit converter.
 */
interface EmbyApi {

    /**
     * Authenticate by username/password. The body is JSON
     * `{"Username":"invitado","Pw":""}` and the [auth] header must be a
     * well-formed `MediaBrowser Client="..." Device="..." DeviceId="..." Version="..."`
     * string. On success the server returns a `User`, an `AccessToken`,
     * a `ServerId` and a `SessionId`.
     */
    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Header("X-Emby-Authorization") auth: String,
        @Body body: EmbyAuthRequest
    ): EmbyAuthResponse

    /** List the user's "views" (libraries: Películas, Series, etc.). */
    @GET("Users/{userId}/Views")
    suspend fun getViews(
        @Path("userId") userId: String,
        @Query("X-Emby-Token") token: String,
        @Query("X-Emby-Client") client: String = "Emby Web",
        @Query("X-Emby-Device-Name") deviceName: String = "Visuales Android",
        @Query("X-Emby-Device-Id") deviceId: String,
        @Query("X-Emby-Client-Version") version: String = "4.9.3.0",
        @Query("X-Emby-Language") language: String = "es"
    ): EmbyViewsResponse

    /**
     * List items visible to the user. Used both for paginated movies and series
     * (filtering by `IncludeItemTypes`) and as a generic browse endpoint.
     */
    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Limit") limit: Int = 50,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Fields") fields: String = "Overview,Genres,ProductionYear,Path,MediaSources",
        @Query("X-Emby-Token") token: String,
        @Query("X-Emby-Client") client: String = "Emby Web",
        @Query("X-Emby-Device-Name") deviceName: String = "Visuales Android",
        @Query("X-Emby-Device-Id") deviceId: String,
        @Query("X-Emby-Client-Version") version: String = "4.9.3.0",
        @Query("X-Emby-Language") language: String = "es"
    ): EmbyItemsResponse

    /** Latest items in a library (parentId). Returns a JSON array, not an object. */
    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Path("userId") userId: String,
        @Query("ParentId") parentId: String,
        @Query("Limit") limit: Int = 12,
        @Query("Fields") fields: String = "BasicSyncInfo,CanDelete,CanDownload,PrimaryImageAspectRatio,ProductionYear",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
        @Query("X-Emby-Token") token: String,
        @Query("X-Emby-Client") client: String = "Emby Web",
        @Query("X-Emby-Device-Name") deviceName: String = "Visuales Android",
        @Query("X-Emby-Device-Id") deviceId: String,
        @Query("X-Emby-Client-Version") version: String = "4.9.3.0",
        @Query("X-Emby-Language") language: String = "es"
    ): List<EmbyItem>

    /** Global search. Returns an `EmbyItemsResponse` (object with `Items` + `TotalRecordCount`). */
    @GET("Items")
    suspend fun searchItems(
        @Query("SearchTerm") searchTerm: String,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Limit") limit: Int = 20,
        @Query("Fields") fields: String = "Overview,Genres,ProductionYear,Path",
        @Query("X-Emby-Token") token: String,
        @Query("X-Emby-Client") client: String = "Emby Web",
        @Query("X-Emby-Device-Name") deviceName: String = "Visuales Android",
        @Query("X-Emby-Device-Id") deviceId: String,
        @Query("X-Emby-Client-Version") version: String = "4.9.3.0",
        @Query("X-Emby-Language") language: String = "es"
    ): EmbyItemsResponse
}

/**
 * Body of `POST /Users/AuthenticateByName`. Field names are PascalCase on
 * purpose: the Emby server expects exactly `Username` and `Pw`.
 */
@Serializable
data class EmbyAuthRequest(
    val Username: String,
    val Pw: String
)
