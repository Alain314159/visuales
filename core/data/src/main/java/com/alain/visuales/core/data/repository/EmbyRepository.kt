package com.alain.visuales.core.data.repository

import android.content.Context
import com.alain.visuales.core.data.db.dao.EmbyItemDao
import com.alain.visuales.core.data.db.entity.EmbyItemEntity
import com.alain.visuales.core.data.preferences.SettingsRepository
import com.alain.visuales.core.network.api.EmbyApi
import com.alain.visuales.core.network.api.EmbyAuthRequest
import com.alain.visuales.core.network.model.EmbyItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public-facing repository for Emby (https://emby.uclv.cu) metadata.
 *
 * Emby is a sibling service of visuales.uclv.cu; both expose the same backend
 * filesystem, so the file paths from visuales.uclv.cu line up with Emby's own
 * `Path` field. We exploit that to "enrich" the catalog UI with posters /
 * overviews / genres / ratings sourced from Emby — without leaving the UCLV
 * network.
 *
 * Responsibilities:
 *  - Lazy login (no-op if a cached token exists; otherwise authenticates as
 *    `invitado` and persists token + userId + deviceId).
 *  - `Flow<EmbyItemEntity?>` lookup by path that reads from the Room cache.
 *  - `refreshByPath(path)` that searches Emby and persists a new cache entry.
 *  - `posterUrl(itemId, tag)` / `backdropUrl(itemId, tag)` builders for Coil.
 *
 * All Emby requests pass `X-Emby-Token` and the other `X-Emby-*` client-id
 * fields as URL query parameters (not HTTP headers). This is what the Emby
 * Web client does and what the server expects for endpoints behind token auth.
 */
@Singleton
class EmbyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: EmbyApi,
    private val settings: SettingsRepository,
    private val embyDao: EmbyItemDao
) {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    @Volatile private var cachedToken: String? = null
    @Volatile private var cachedUserId: String? = null
    @Volatile private var deviceId: String? = null

    /**
     * Make sure we have a token + userId + deviceId, performing a network
     * login only if needed. Returns `(token, userId)`.
     */
    private suspend fun ensureLoggedIn(): Pair<String, String> {
        cachedToken?.let { tok ->
            cachedUserId?.let { uid -> return tok to uid }
        }
        // Try DataStore first.
        val s = settings.settings.first()
        if (!s.embyToken.isNullOrEmpty() && !s.embyUserId.isNullOrEmpty()) {
            cachedToken = s.embyToken
            cachedUserId = s.embyUserId
            deviceId = s.embyDeviceId ?: generateDeviceId().also { settings.setEmbyDeviceId(it) }
            _loginState.value = LoginState.LoggedIn("invitado")
            return cachedToken!! to cachedUserId!!
        }
        return login()
    }

    /** Perform `POST /Users/AuthenticateByName` as `invitado`. */
    private suspend fun login(): Pair<String, String> {
        _loginState.value = LoginState.LoggingIn
        val devId = settings.settings.first().embyDeviceId ?: generateDeviceId().also { id ->
            settings.setEmbyDeviceId(id)
        }
        deviceId = devId
        val auth = "MediaBrowser Client=\"Visuales\", Device=\"Android\", DeviceId=\"$devId\", Version=\"2.0\""
        val resp = try {
            api.authenticateByName(auth, EmbyAuthRequest("invitado", ""))
        } catch (e: Exception) {
            _loginState.value = LoginState.Failed(e.message ?: e.toString())
            throw e
        }
        cachedToken = resp.AccessToken
        cachedUserId = resp.User.Id
        settings.setEmbyToken(resp.AccessToken)
        settings.setEmbyUserId(resp.User.Id)
        _loginState.value = LoginState.LoggedIn(resp.User.Name)
        return resp.AccessToken to resp.User.Id
    }

    /** List the user's libraries (Películas, Series, etc.). */
    suspend fun getViews(): List<EmbyItem> {
        val (token, userId) = ensureLoggedIn()
        return try {
            api.getViews(userId, token, deviceId = deviceId!!).Items
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Latest items in a library. */
    suspend fun getLatestItems(parentId: String, limit: Int = 12): List<EmbyItem> {
        val (token, userId) = ensureLoggedIn()
        return try {
            api.getLatestItems(userId, parentId, limit, token = token, deviceId = deviceId!!)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Paginated movies. */
    suspend fun getMovies(limit: Int = 50, startIndex: Int = 0): List<EmbyItem> {
        val (token, userId) = ensureLoggedIn()
        return try {
            api.getItems(
                userId,
                includeItemTypes = "Movie",
                limit = limit,
                startIndex = startIndex,
                token = token,
                deviceId = deviceId!!
            ).Items
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Paginated series. */
    suspend fun getSeries(limit: Int = 50, startIndex: Int = 0): List<EmbyItem> {
        val (token, userId) = ensureLoggedIn()
        return try {
            api.getItems(
                userId,
                includeItemTypes = "Series",
                limit = limit,
                startIndex = startIndex,
                token = token,
                deviceId = deviceId!!
            ).Items
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Global search. */
    suspend fun search(query: String, limit: Int = 20): List<EmbyItem> {
        val (token, _) = ensureLoggedIn()
        return try {
            api.searchItems(query, limit = limit, token = token, deviceId = deviceId!!).Items
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Observe the cached [EmbyItemEntity] for [path] (the visuales.uclv.cu
     * file path). Returns `null` until [refreshByPath] has populated the row
     * (or the row expired and was pruned).
     */
    fun lookupByPath(path: String): Flow<EmbyItemEntity?> = embyDao.findByPath(path)

    /**
     * Search Emby by the basename of [path] and persist a fresh cache entry
     * for the best match. Returns the persisted entity, or `null` if no
     * candidate was found.
     */
    suspend fun refreshByPath(path: String): EmbyItemEntity? {
        val (token, _) = ensureLoggedIn()
        val name = path.substringAfterLast('/').substringBeforeLast('.')
        if (name.isBlank()) return null
        val results = search(name)
        if (results.isEmpty()) return null
        // Prefer a result whose Emby Path contains / is contained by the
        // visuales.uclv.cu path; fall back to the first result otherwise.
        val match = results.firstOrNull { it.Path?.let { p -> p.contains(path) || path.contains(p) } == true }
            ?: results.first()
        val entity = EmbyItemEntity(
            itemId = match.Id,
            path = path,
            name = match.Name,
            type = match.Type ?: "",
            overview = match.Overview,
            genres = match.Genres?.joinToString(",") ?: "",
            productionYear = match.ProductionYear,
            posterTag = match.ImageTags?.Primary,
            backdropTag = match.BackdropImageTags?.firstOrNull(),
            communityRating = match.CommunityRating,
            officialRating = match.OfficialRating,
            runtimeMs = match.durationMs,
            cachedAt = System.currentTimeMillis(),
            ttlExpiresAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000  // 7 days
        )
        embyDao.upsert(entity)
        return entity
    }

    /** Build the URL of the primary poster image (for Coil). */
    fun posterUrl(itemId: String, tag: String?): String? {
        if (tag == null) return null
        return buildImageUrl(itemId, "Primary", tag)
    }

    /** Build the URL of a backdrop image (for Coil). */
    fun backdropUrl(itemId: String, tag: String?): String? {
        if (tag == null) return null
        return buildImageUrl(itemId, "Backdrop", tag)
    }

    private fun buildImageUrl(itemId: String, type: String, tag: String): String {
        val tok = cachedToken ?: ""
        val dev = deviceId ?: ""
        // Spaces must be `+` (form-encoded) for Emby to accept the request when
        // these are passed as URL query parameters. Coil will handle the
        // resulting URL as-is.
        return "https://emby.uclv.cu/emby/Items/$itemId/Images/$type" +
            "?tag=$tag" +
            "&X-Emby-Token=$tok" +
            "&X-Emby-Client=Emby+Web" +
            "&X-Emby-Device-Name=Visuales+Android" +
            "&X-Emby-Device-Id=$dev" +
            "&X-Emby-Client-Version=4.9.3.0" +
            "&X-Emby-Language=es"
    }

    private fun generateDeviceId(): String = java.util.UUID.randomUUID().toString()

    /** Lifecycle of the lazy login. */
    sealed interface LoginState {
        data object Idle : LoginState
        data object LoggingIn : LoginState
        data class LoggedIn(val username: String) : LoginState
        data class Failed(val error: String) : LoginState
    }
}
