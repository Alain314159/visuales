package com.alain.visuales.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `POST /Users/AuthenticateByName`.
 *
 * Field names are PascalCase on purpose — Emby's JSON uses PascalCase
 * consistently across all endpoints.
 */
@Serializable
data class EmbyAuthResponse(
    val User: EmbyUser,
    val AccessToken: String,
    val ServerId: String,
    val SessionId: String? = null
)

@Serializable
data class EmbyUser(
    val Name: String,
    val Id: String,
    val HasPassword: Boolean = false,
    @SerialName("Policy") val policy: EmbyUserPolicy? = null
)

@Serializable
data class EmbyUserPolicy(
    val IsAdministrator: Boolean = false,
    val IsHidden: Boolean = false,
    val IsDisabled: Boolean = false,
    val EnableLiveTvAccess: Boolean = false
)

@Serializable
data class EmbyViewsResponse(
    val Items: List<EmbyItem> = emptyList()
)

@Serializable
data class EmbyItemsResponse(
    val Items: List<EmbyItem> = emptyList(),
    val TotalRecordCount: Int = 0
)

/**
 * A single Emby item — a movie, series, episode, folder, etc.
 *
 * Only the fields we actually use are declared; `ignoreUnknownKeys = true`
 * is set on the kotlinx-serialization Json instance in [com.alain.visuales.core.network.NetworkModule]
 * so any extra server-side fields (and there are many) are silently dropped.
 */
@Serializable
data class EmbyItem(
    val Name: String,
    val Id: String,
    val Type: String? = null,
    val Path: String? = null,
    val Overview: String? = null,
    val Genres: List<String>? = null,
    val ProductionYear: Int? = null,
    val ImageTags: EmbyImageTags? = null,
    val BackdropImageTags: List<String>? = null,
    val RunTimeTicks: Long? = null,  // 10000 ticks = 1 ms
    val CommunityRating: Double? = null,
    val OfficialRating: String? = null,
    val ProviderIds: Map<String, String>? = null,  // Imdb, Tmdb, etc.
    val MediaType: String? = null,
    val IsFolder: Boolean = false
) {
    /** Convenience: convert Emby's 100-ns ticks to milliseconds. */
    val durationMs: Long? get() = RunTimeTicks?.let { it / 10000 }
}

@Serializable
data class EmbyImageTags(
    val Primary: String? = null,
    val Art: String? = null,
    val Banner: String? = null,
    val Logo: String? = null,
    val Thumb: String? = null,
    val Disc: String? = null,
    val Menu: String? = null
)

@Serializable
data class EmbySystemInfo(
    val ServerName: String,
    val Version: String,
    val Id: String,
    val LocalAddresses: List<String> = emptyList(),
    val RemoteAddresses: List<String> = emptyList()
)
