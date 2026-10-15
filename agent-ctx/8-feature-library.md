# Task ID: 8 — Feature Library

**Agent name:** feature-library (subagent of Task 8)
**Module:** `:feature:library` (Kotlin / Jetpack Compose / Hilt)
**Repo root:** `/home/z/my-project/repos/visuales-kotlin`

## Goal

Implement the full user-facing library feature: a grid of video thumbnails
backed by a Hilt `LibraryViewModel`, a Material 3 `LibraryCard` with thumbnail
placeholder / filename / size + duration meta / watch progress / watched
badge / long-press context menu, plus pull-to-refresh, search, sort dropdown
and empty state.

## Existing code consumed as-is

| Symbol | Source | Purpose |
|---|---|---|
| `LibraryRepository.observeAll()` | `core/data/.../LibraryRepository.kt` | live stream of `LibraryItemEntity` |
| `LibraryRepository.refresh()`    | same | filesystem re-scan → Room upsert |
| `LibraryRepository.setWatched(path, watched)` | same | toggles `is_watched` column |
| `LibraryRepository.delete(path)` | same | deletes file on disk + Room row |
| `LibraryItemEntity`              | `core/data/.../db/entity/LibraryItemEntity.kt` | fields: path (PK), name, size, durationMs?, thumbnailPath?, watchProgressMs, isWatched, addedAt, lastPlayedAt? |
| `EmptyState(icon, title, message, modifier, action)` | `core/designsystem/.../StateComponents.kt` | empty-state composable used by Downloads screen too |
| `formatBytes(bytes)`             | `core/common/.../Format.kt` | human-readable file size |
| `LIBRARY_GRAPH`, `LIBRARY_ROUTE`, `libraryGraph(...)` | `feature/library/.../navigation/LibraryNavigation.kt` | unchanged — still wires `LibraryScreen(modifier = Modifier)` into the nav graph |
| FileProvider authority `${applicationId}.fileprovider` | `:app/AndroidManifest.xml` | already declared with `file_paths.xml` meta-data |

## Files created / replaced

All paths under `feature/library/src/main/java/com/alain/visuales/feature/library/`:

| File | Action | Purpose |
|---|---|---|
| `screens/LibraryViewModel.kt` | NEW | `@HiltViewModel class LibraryViewModel @Inject constructor(repo: LibraryRepository)`. Exposes `items`, `searchQuery`, `sortMode`, `isRefreshing` `StateFlow`s + `setSearchQuery`, `setSortMode`, `refresh`, `play(context, item)`, `toggleWatched`, `delete`. Also defines top-level `enum class SortMode { DATE_ADDED, NAME, SIZE, LAST_PLAYED }`. |
| `components/LibraryCard.kt`  | NEW | Material 3 `Card` with: 16:9 thumbnail placeholder (Icons.Filled.Film on surface), filename (2 lines, ellipsis), meta row ("1.4 GB · 1h 32m"), watch progress bar (only if `durationMs > 0`), "watched" badge (top-right CheckCircle on black scrim) and `combinedClickable` long-press → local `DropdownMenu` with "Marcar como visto/no visto" + "Eliminar". |
| `screens/LibraryScreen.kt`  | REPLACED (placeholder) | `@Composable fun LibraryScreen(modifier, viewModel = hiltViewModel())`. Scaffold + TopAppBar ("Biblioteca" + Sort overflow) + OutlinedTextField search + `PullToRefreshBox` wrapping either `EmptyState` (Icons.Outlined.Movie, "Tu biblioteca está vacía", "Las películas que descargues aparecerán aquí.") or `LazyVerticalGrid(GridCells.Adaptive(150.dp))` of `LibraryCard`s. Delete confirmed via `AlertDialog`. |
| `navigation/LibraryNavigation.kt` | UNCHANGED | already calls `LibraryScreen(modifier = Modifier)`. The new screen keeps the same signature so the wiring is unchanged. |
| `build.gradle.kts` | UNCHANGED | already declares Hilt (plugin + kapt), Compose BOM, material3, material-icons-extended, navigation-compose, hilt-navigation-compose, coil-compose, core-ktx, lifecycle-runtime-compose, lifecycle-viewmodel-compose, coroutines, and the `:core:common`, `:core:designsystem`, `:core:data`, `:core:network` project deps. No new deps needed for this task. |

## Key design decisions

1. **`coil-video` deferred** — the task explicitly says "since `coil-video` is not in deps, just use a placeholder icon (Film) with the file name." So the thumbnail area is currently a `Box(Modifier.aspectRatio(16f/9f).background(surface))` with an `Icons.Filled.Film` icon centered. When `coil-video` is added later, the same `Box` can host an `AsyncImage` loading a `File(item.path)` URI via a `VideoFrameDecoder`. The card's public API doesn't change.

2. **Search + sort in the ViewModel** — the `items` `StateFlow` is derived via `combine(repo.observeAll(), searchQuery, sortMode) { all, q, sort -> applyFiltersAndSort(all, q, sort) }`. This means the UI never has to re-derive the filtered+sorted list; it just collects `items`. The pure `applyFiltersAndSort(...)` helper is pulled out so it can be unit-tested in isolation.

3. **Sort semantics**
   - `DATE_ADDED`  → `sortedByDescending { it.addedAt }` (newest first)
   - `NAME`        → `sortedBy { it.name.lowercase() }` (alphabetical, case-insensitive)
   - `SIZE`        → `sortedByDescending { it.size }` (largest first)
   - `LAST_PLAYED` → `sortedByDescending { it.lastPlayedAt ?: 0L }` (nulls last)

4. **Search is case-insensitive substring match on `item.name`** — no diacritic folding (matches what `LibraryRepository.search()` does at the DAO level; we chose to filter in-process so the user gets the same match logic regardless of whether the DAO FTS query matches).

5. **Pull-to-refresh actually does work** — unlike the Downloads screen (where the underlying flows are already live and refresh is purely a UX affordance), here `LibraryViewModel.refresh()` calls `repo.refresh()` which performs a real filesystem scan via `LibraryScanner.scan()` and `dao.upsertAll(items)`. The `_isRefreshing` flag is flipped on for at least `MIN_REFRESH_SPIN_MS = 500 ms` (so the spinner is always visible briefly) and turned off in a `finally` block. The function is also reentrancy-guarded (`if (_isRefreshing.value) return`).

6. **Sort dropdown uses an `Icons.Filled.Sort` action icon** in the TopAppBar (not `MoreVert`), because it's the only overflow action and a dedicated sort icon is clearer. The current mode is decorated with a "✓ " prefix in the dropdown items so the user can see which sort is active at a glance.

7. **Long-press menu is anchored at the card** — `LibraryCard` itself owns the `DropdownMenu` (visibility tracked via internal `var menuExpanded`). Long-press flips `menuExpanded = true` and also fires the `onLongPress: () -> Unit = {}` callback (default no-op) so the screen can react if it ever wants to. The menu has two items: "Marcar como visto/no visto" (label depends on current state) → `onToggleWatched()`, and "Eliminar" (error-tinted) → `onDelete()`.

8. **Delete confirmation** — `LibraryScreen` keeps a `var pendingDelete: LibraryItemEntity?` state. When the card's "Eliminar" menu item is tapped, the screen sets `pendingDelete = item`, which renders an `AlertDialog` asking "¿Seguro que quieres eliminar \"<name>\"? El archivo se borrará del dispositivo." with "Eliminar" (error-tinted) / "Cancelar" buttons. The repo's `delete(path)` actually deletes the file from disk via `java.io.File(path).delete()` plus the Room row, so the confirmation step is important to avoid accidental data loss.

9. **Play uses FileProvider + ACTION_VIEW** — same pattern as `DownloadsViewModel.playVideo()`:
   - `File(item.path)` (the entity already stores an absolute filesystem path).
   - `FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)` — authority matches the `<provider>` declared in `:app/AndroidManifest.xml`.
   - `Intent(ACTION_VIEW).setDataAndType(uri, "video/*").addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK)`.
   - Three failure paths each show a Toast: file missing ("Archivo no encontrado"), path outside FileProvider's declared paths ("No se puede abrir el archivo"), no activity can handle `video/*` ("No hay reproductor de video").

10. **`formatDuration(ms: Long?)` helper inside `LibraryCard.kt`** — we deliberately DON'T reuse `formatEta` from `:core:common` because that helper returns "Xm Ys" for sub-hour durations, which is too noisy for a grid card. Our helper returns:
    - `""`        if `ms == null || ms <= 0` (card then hides the meta row entirely if there's also no size).
    - `Xh Ym`     if ≥ 1 hour.
    - `Xm`        if ≥ 1 minute (no seconds).
    - `Xs`        only for sub-minute clips.

11. **Watch progress bar** uses Material 3's lambda-based `LinearProgressIndicator(progress = { ... })` (introduced in Material 3 1.3.0; same as the `DownloadCard`). Track color is `surface` so the bar visually "cuts into" the surfaceVariant card. Bar is only drawn when `durationMs > 0` AND `watchProgressMs > 0` — `watchProgressFraction(...)` returns `null` otherwise.

12. **Empty state wrapped in `verticalScroll`** so the `PullToRefreshBox` gesture still triggers when the grid is empty (same trick as `DownloadsScreen`). Otherwise the gesture wouldn't have anything to drag against and the pull-to-refresh indicator would never appear.

13. **Grid uses `GridCells.Adaptive(150.dp)`** — yields ~2 columns on phones, 3+ on tablets, automatically. The task spec mentions "2 columns on phones, 3+ on tablets" — adaptive sizing gives us this without manual `windowSizeClass` plumbing.

14. **Card key = `it.path`** (the entity PK) — so `LazyVerticalGrid` correctly diffs cards across refreshes / watched toggles / deletes without flinching.

## Constraints honored

- All files under `feature/library/...`. ✓
- Hilt: `@HiltViewModel class LibraryViewModel @Inject constructor(...)`. ✓
- `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(item.path))` and `Intent(ACTION_VIEW).setDataAndType(uri, "video/*").addFlags(FLAG_GRANT_READ_URI_PERMISSION)`. ✓ (we also add `FLAG_ACTIVITY_NEW_TASK` because we launch from an Application context — required by Android when starting an Activity from a non-Activity context.)
- `formatBytes` from `:core:common` is used (in `LibraryCard.buildMetaString`). ✓
- Material Icons Extended: `Icons.Filled.Film`, `Icons.Filled.CheckCircle`, `Icons.Filled.Search`, `Icons.Filled.Sort`, `Icons.Filled.Clear`, `Icons.Outlined.Movie`. ✓
- Spanish hardcoded strings: "Biblioteca", "Buscar", "Ordenar", "Limpiar búsqueda", "Marcar como visto", "Marcar como no visto", "Eliminar", "Eliminar video", "Cancelar", "Tu biblioteca está vacía", "Las películas que descargues aparecerán aquí.", "Archivo no encontrado", "No se puede abrir el archivo", "No hay reproductor de video", "Fecha de adición", "Nombre", "Tamaño", "Última reproducción". ✓
- Long-press menu: `DropdownMenu` anchored at the card. ✓
- Did NOT modify any existing files in `:core:*` or `:app`. ✓
- Did NOT try to compile. ✓

## Verification

```bash
$ ls -R feature/library/src/main/java/
feature/library/src/main/java/:
com

feature/library/src/main/java/com:
alain

feature/library/src/main/java/com/alain:
visuales

feature/library/src/main/java/com/alain/visuales:
feature

feature/library/src/main/java/com/alain/visuales/feature:
library

feature/library/src/main/java/com/alain/visuales/feature/library:
components
navigation
screens

feature/library/src/main/java/com/alain/visuales/feature/library/components:
LibraryCard.kt

feature/library/src/main/java/com/alain/visuales/feature/library/navigation:
LibraryNavigation.kt

feature/library/src/main/java/com/alain/visuales/feature/library/screens:
LibraryScreen.kt
LibraryViewModel.kt
```

## Files NOT modified (deliberately)

- `feature/library/src/main/java/com/alain/visuales/feature/library/navigation/LibraryNavigation.kt` — already wires `LibraryScreen(modifier = Modifier)` into `libraryGraph`; the new screen keeps the same signature.
- `feature/library/build.gradle.kts` — already declares Hilt, Compose BOM, material3, material-icons-extended, navigation-compose, hilt-navigation-compose, coil-compose, core-ktx, lifecycle-runtime-compose, lifecycle-viewmodel-compose, coroutines. No new dependencies needed.
- `:app/AndroidManifest.xml` — already declares the `FileProvider` with `${applicationId}.fileprovider` authority + `file_paths.xml`. The provider's `external-path`, `external-files-path`, etc. cover the typical library file locations.
- `:core:data/.../LibraryRepository.kt` and `LibraryDao.kt` — exposed `observeAll`, `refresh`, `setWatched`, `delete`. All consumed as-is.
- `:core:designsystem/.../StateComponents.kt` — `EmptyState` consumed as-is.

## Next actions (not in scope of this task)

- Add `io.coil-kt.coil3:coil-video` to `:feature:library/build.gradle.kts` and switch `LibraryCard`'s thumbnail `Box` to an `AsyncImage` with `VideoFrameDecoder` so the first frame of each video is loaded as the actual thumbnail. The card's public API doesn't need to change.
- Persist `lastPlayedAt` from the player screen by calling `LibraryRepository.updateWatchProgress(path, progressMs)` on playback position updates (this method already exists in the repo and updates both `watch_progress_ms` and `last_played_at`).
- Surface a snackbar after a successful delete ("Archivo eliminado") with an "Deshacer" action that re-inserts the row (today, deletes are silent; the file is gone so undo isn't really possible without a trash-can pattern).
- Add a "multi-select" mode (long-press to enter selection mode, then checkmarks on multiple cards with a bulk delete action).
