# Task ID: 14 — Emby Integration

**Agent name:** emby-integration (subagent of Task 14)
**Module:** cross-module (`:core:network`, `:core:data`, `:feature:catalog`, `:feature:settings`)
**Repo root:** `/home/z/my-project/repos/visuales-kotlin`

## Goal

Implement full Emby (`emby.uclv.cu` 4.9.3.0) integration so the catalog UI
can be enriched with posters / overviews / genres / ratings / runtimes sourced
from Emby. Emby is a sibling service of `visuales.uclv.cu` exposing the same
backend filesystem; we exploit the matching `Path` field to look up items by
their visuales.uclv.cu file path.

## Captured auth profile

- Endpoint: `https://emby.uclv.cu/emby/`
- Login: `POST /Users/AuthenticateByName` body `{"Username":"invitado","Pw":""}`
  with `X-Emby-Authorization` header
  `MediaBrowser Client="Visuales", Device="Android", DeviceId="<id>", Version="2.0"`.
- All subsequent requests pass `X-Emby-Token`, `X-Emby-Client`,
  `X-Emby-Device-Name`, `X-Emby-Device-Id`, `X-Emby-Client-Version`,
  `X-Emby-Language` as **URL query parameters** (this is what the Emby Web
  client does — not HTTP headers).
- UserId seen in production: `5f24aa48d8f64947860a5169c7a82e35`.
- Películas ParentId: `3`; Series ParentId: `66923` (not used directly by
  this task — we filter via `IncludeItemTypes=Movie|Series` instead).

## Files created

| File | Action | Purpose |
|---|---|---|
| `core/network/.../api/EmbyApi.kt` | NEW | Retrofit interface with five endpoints (`authenticateByName`, `getViews`, `getItems`, `getLatestItems`, `searchItems`) + a top-level `@Serializable EmbyAuthRequest(Username, Pw)` DTO. Uses `@Query` for every `X-Emby-*` field per the Emby Web convention; only `authenticateByName` takes a real `@Header("X-Emby-Authorization")`. |
| `core/network/.../model/EmbyDtos.kt` | NEW | `@Serializable` DTOs for `EmbyAuthResponse`, `EmbyUser`, `EmbyUserPolicy`, `EmbyViewsResponse`, `EmbyItemsResponse`, `EmbyItem` (with `durationMs` derived prop from `RunTimeTicks / 10000`), `EmbyImageTags`, `EmbySystemInfo`. PascalCase field names match the Emby server exactly. |
| `core/data/.../db/entity/EmbyItemEntity.kt` | NEW | Room entity (table `emby_items`) keyed by `item_id`, with indices on `path` (for lookup-by-path) and `ttl_expires_at` (for cleanup). Caches poster tag, backdrop tag, overview, genres (CSV), productionYear, communityRating, officialRating, runtimeMs. 7-day TTL managed by the caller. |
| `core/data/.../db/dao/EmbyItemDao.kt` | NEW | Room DAO with `upsert`, `upsertAll`, `findByPath` (Flow), `getById`, `getExpired`, `deleteExpired`, `clearAll`. |
| `core/data/.../repository/EmbyRepository.kt` | NEW | `@Singleton` repository. Lazy `ensureLoggedIn()` that re-uses a cached token or re-logs in as `invitado`. Exposes `getViews`, `getLatestItems`, `getMovies`, `getSeries`, `search`, `lookupByPath(path): Flow<EmbyItemEntity?>`, `refreshByPath(path)` (search by basename → first `Path` match → upsert), `posterUrl(itemId, tag)` and `backdropUrl(itemId, tag)` builders for Coil. `LoginState` sealed interface (`Idle` / `LoggingIn` / `LoggedIn(name)` / `Failed(error)`) exposed as `StateFlow`. |

## Files edited

| File | Change |
|---|---|
| `core/data/.../db/VisualesDatabase.kt` | Added `EmbyItemEntity` to `entities` array, `EmbyItemDao` import, `abstract fun embyItemDao()` declaration, `provideEmbyItemDao(db)` Hilt provider, and **bumped `version = 1` → `2`**. `fallbackToDestructiveMigration()` (already on the builder) handles the schema change at runtime. |
| `core/network/.../NetworkModule.kt` | Added three `@Provides` methods: `provideEmbyJson()` (lenient `Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }`), `provideEmbyApi(client, json)` (separate Retrofit instance with `baseUrl("https://emby.uclv.cu/emby/")` + `Json.asConverterFactory("application/json".toMediaType())`). The existing `provideRetrofit` is unchanged because `VisualesApi` only returns raw HTML strings — adding the converter there would be a no-op but also harmless; kept separate to minimize blast radius. |
| `core/data/.../preferences/SettingsRepository.kt` | Added four `UserSettings` fields (`embyEnabled: Boolean = true`, `embyToken: String? = null`, `embyUserId: String? = null`, `embyDeviceId: String? = null`), four DataStore keys (`KEY_EMBY_ENABLED`, `KEY_EMBY_TOKEN`, `KEY_EMBY_USER_ID`, `KEY_EMBY_DEVICE_ID`), and four suspend mutators (`setEmbyEnabled`, `setEmbyToken`, `setEmbyUserId`, `setEmbyDeviceId`). The nullable-string mutators remove the key when passed `null`. |
| `feature/catalog/.../screens/CatalogViewModel.kt` | Added `EmbyRepository` + `SettingsRepository` to `@Inject` constructor. Added `_embyPosters: MutableStateFlow<Map<String, EmbyItemEntity?>>` + public `embyPosters: StateFlow<...>`. Added `requestEmbyLookup(path: String)` which (a) honors `settings.embyEnabled` and short-circuits when off, (b) reads the Room cache via `lookupByPath(path).first()`, (c) falls back to `embyRepo.refreshByPath(path)` (network search + write). Added `posterUrlFor(path)` convenience for the UI to build a Coil URL. |
| `feature/settings/.../screens/SettingsViewModel.kt` | Added `setEmbyEnabled(enabled)` delegate and `repo.setEmbyEnabled(true)` to the `resetSettings()` list. |
| `feature/settings/.../screens/SettingsScreen.kt` | Added a new `ToggleRow("Usar Emby para metadatos", subtitle = "Portadas, sinopsis y géneros desde emby.uclv.cu", checked = settings.embyEnabled, onChange = viewModel::setEmbyEnabled, icon = Icons.Outlined.VideoLibrary)` in the "Catálogo" section, between "Metadatos enriquecidos" and "Última sincronización". |
| `gradle/libs.versions.toml` | Added `retrofit-serialization-converter` library entry pointing at `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0` (the `serializationConverter` version was already declared in the catalog but had no library alias referencing it). |
| `core/network/build.gradle.kts` | Added `implementation(libs.retrofit.serialization.converter)` to the HTTP deps block. The `kotlinx.serialization` plugin was already applied so the `@Serializable` DTOs compile. |

## Files verified but unchanged

- `app/src/main/AndroidManifest.xml` — already references
  `@xml/network_security_config` (line 54) instead of using the coarse
  `android:usesCleartextTraffic` attribute. The network security config is the
  recommended, more granular approach and takes precedence over the boolean.
- `app/src/main/res/xml/network_security_config.xml` — already permits cleartext
  to `uclv.cu` (with `includeSubdomains="true"`) and `uclv.edu.cu`; this covers
  `emby.uclv.cu`. Emby itself uses HTTPS so cleartext is irrelevant, but the
  config is correct regardless.
- `feature/catalog/build.gradle.kts` — already declares `:core:network` and
  `:core:data` so the new `EmbyRepository` / `EmbyItemEntity` / `SettingsRepository`
  symbols resolve without any change.

## Decisions made

- **Separate Retrofit instance for Emby** instead of retrofit-converter-on-shared-retrofit:
  the existing `provideRetrofit` returns a Retrofit configured with
  `AppConstants.DEFAULT_BASE_URL` (visuales.uclv.cu) and no converter factory
  (the `VisualesApi` interface only returns `String` / `Response` / `ResponseBody`).
  Reusing it would either change its base URL (breaking `VisualesApi`) or add an
  unused converter factory (cruft). A separate instance keeps the blast radius
  minimal.
- **Lenient `Json` instance**: Emby returns dozens of fields per item we don't
  model (e.g. `Tags`, `Studios`, `People`, `Chapters`, `MediaStreams`).
  `ignoreUnknownKeys = true` is mandatory; `coerceInputValues = true` lets the
  decoder fall back to defaults when a value type doesn't match (e.g. `"null"`
  string for an Int field); `explicitNulls = false` keeps our `null` defaults
  when the server omits the field entirely.
- **PascalCase DTO field names**: deliberately NOT using `@SerialName` for the
  common fields because Emby's JSON uses PascalCase (`Name`, `Id`, `Overview`,
  `Genres`, `ProductionYear`, `ImageTags`, `BackdropImageTags`, `RunTimeTicks`,
  `CommunityRating`, `OfficialRating`, `ProviderIds`, `MediaType`, `IsFolder`).
  Keeping the Kotlin property names PascalCase makes the DTOs round-trip
  without any annotation noise. (`EmbyUser.policy` uses `@SerialName("Policy")`
  only because the property name is lower-case to match Kotlin conventions for
  nested data — without the annotation, kotlinx-serialization would look for
  `policy` in the JSON and fail.)
- **URL query params instead of headers for `X-Emby-*`**: documented in the
  EmbyApi KDoc and implemented as `@Query` annotations on every authenticated
  endpoint. This is what the captured Emby Web session does. The single
  exception is the `X-Emby-Authorization` header on `POST /Users/AuthenticateByName`
  — at that point the token has not been issued yet, so it can't go in the URL.
- **`RunTimeTicks`**: Emby stores duration in 100-ns ticks (Windows FILETIME
  convention). The `EmbyItem.durationMs` extension property divides by 10000
  to give milliseconds; the cache entity stores `runtimeMs` so consumers don't
  have to remember the unit.
- **`Map<String, EmbyItemEntity?>` for posters**: the value is nullable so the
  UI can distinguish "still loading / no match found" (null) from "never asked"
  (absent from the map). When the UI calls `requestEmbyLookup(path)` it gets a
  non-null entry in the map immediately after the cache check, even if that
  entry is null (signaling "no match").
- **One-shot `lookupByPath(path).first()` instead of subscribing to the Room
  flow**: simpler, cheaper, and matches the v1 use case (we just want the
  poster; we don't need live updates if the cache is invalidated mid-scroll).
  If the user pulls to refresh and the TTL expires, the next `requestEmbyLookup`
  will read a fresh row.
- **`embyLookedUp.add(path)` re-entrancy guard**: prevents the same path from
  being looked up twice within one VM lifetime (e.g. when the user scrolls up
  and down). The map's `_embyPosters.value[path]` is the source of truth for
  the UI; the guard only prevents duplicate network calls.
- **`device-id` persistence**: generated once with `UUID.randomUUID()` and
  written to DataStore via `setEmbyDeviceId(...)` on first login. Subsequent
  app starts reuse it. This is what Emby expects for device-tracking; using a
  fresh random ID per launch would create a separate session each time, which
  is fine functionally but messy on the Emby admin dashboard.
- **Database version bump 1 → 2**: adding a new entity requires a schema change.
  `fallbackToDestructiveMigration()` is already set on the Room builder (it
  was a deliberate choice for v1 of this app — see Task ID 4/8 worklog). The
  bump triggers a destructive recreate on next launch, which is acceptable
  because the existing tables (directories, downloads, library_items) are
  repopulated from the network / filesystem on next sync anyway. If we ever
  want to preserve user data on schema changes, we should switch to a real
  Migration before the next entity add.
- **`setEmbyToken(null)` and `setEmbyUserId(null)` remove the key** instead of
  writing `null`: DataStore Preferences has no native null, so `remove(key)`
  is the canonical "clear" operation. The next read returns the
  default (`null`) via the `?:` Elvis fallback in the mapping.

## Build verification notes

- `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0`
  transitively depends on `kotlinx-serialization-json` (already declared
  via `libs.kotlinx.serialization.json`) and `retrofit` (already declared via
  `libs.retrofit`). No additional transitive deps are pulled in.
- The `retrofit2.converter.kotlinx.serialization.asConverterFactory` import
  resolves to the extension function provided by the converter library.
- The `okhttp3.MediaType.Companion.toMediaType` import is provided by OkHttp
  (already on the classpath via `libs.okhttp`).
- The `kotlinx.serialization.json.Json` import is provided by
  `kotlinx-serialization-json` (already in `core/network/build.gradle.kts`).
- `EmbyItemEntity`'s `@Index("path")` and `@Index("ttl_expires_at")` generate
  the expected `CREATE INDEX` statements; Room KSP processes them via the
  existing `ksp(libs.androidx.room.compiler)` declaration in
  `core/data/build.gradle.kts`.
- The Hilt `@Singleton` annotations on `EmbyRepository` and the `provideEmbyApi`
  / `provideEmbyJson` providers will be picked up because both modules
  (`:core:network`, `:core:data`) apply `alias(libs.plugins.hilt)` +
  `kapt(libs.hilt.compiler)`.
- The `:feature:catalog` module already has `implementation(project(":core:data"))`
  and `implementation(project(":core:network"))` so the new
  `EmbyRepository` / `EmbyItemEntity` / `SettingsRepository` symbols resolve.
  No `build.gradle.kts` change in `:feature:catalog` was needed.
- The `:feature:settings` module likewise already depends on `:core:data` so
  the new `UserSettings.embyEnabled` field is visible to `SettingsViewModel`.
- The `UserSettings(...)` initial value in `SettingsViewModel.kt` does not
  specify the new `embyEnabled` / `embyToken` / `embyUserId` / `embyDeviceId`
  fields because they all have defaults in the data class definition
  (`embyEnabled = true`, others `= null`). The Kotlin compiler allows
  omitting defaulted named parameters, so the existing constructor call still
  compiles.

## What this enables for the UI

The catalog screens can now, for each visible directory:

```kotlin
val embyPosters by viewModel.embyPosters.collectAsStateWithLifecycle()
LaunchedEffect(directory.path) { viewModel.requestEmbyLookup(directory.path) }
val posterUrl = viewModel.posterUrlFor(directory.path)
// → Coil AsyncImage(model = posterUrl, ...)
```

When `settings.embyEnabled` is `false`, `requestEmbyLookup` returns
immediately without any network call — so the user can opt out via the new
"Usar Emby para metadatos" toggle in Settings → Catálogo.

## What is NOT in scope (future work)

- Wiring `posterUrlFor` into `DirectoryCard` / `HeroCard` (the existing
  `DirectoryCard(posterUrl?)` parameter is already there, commented as
  "future MediaItemDetector plug-in"; that wiring belongs to a follow-up
  UI task).
- Displaying the Emby overview / genres / rating as a "Detail" sheet
  (DetailScreen was deliberately dropped in Task 6 — see worklog).
- Browsing Emby's own libraries (Películas/Series) as a separate "browse by
  metadata" surface — could be a future alternative to the visuales.uclv.cu
  path-based browsing model.
- TTL-based cache pruning worker: the DAO exposes `deleteExpired(now)` but no
  `WorkManager` job calls it yet. The 7-day TTL is currently best-effort.
- Token refresh on 401: the lazy login only refreshes the in-memory cached
  values; if the server invalidates the token mid-session, requests will
  silently return empty lists. A 401-aware OkHttp interceptor that calls
  `login()` again would handle this.
