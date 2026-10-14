package com.alain.visuales.feature.library.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alain.visuales.core.data.db.entity.LibraryItemEntity
import com.alain.visuales.core.designsystem.components.EmptyState
import com.alain.visuales.feature.library.components.LibraryCard

/**
 * Top-level screen for the Library feature.
 *
 * Layout:
 *
 * ```
 * ┌──────────────────────────────────────────┐
 * │ TopAppBar: "Biblioteca"        [Sort ⌄]  │
 * ├──────────────────────────────────────────┤
 * │ [🔍 Buscar............................]   │
 * ├──────────────────────────────────────────┤
 * │                                          │
 * │  LazyVerticalGrid (adaptive ~150 dp)     │  ← pull-to-refresh
 * │  ┌────────┐ ┌────────┐ ┌────────┐        │
 * │  │ card   │ │ card   │ │ card   │        │
 * │  └────────┘ └────────┘ └────────┘        │
 * │  ...                                     │
 * └──────────────────────────────────────────┘
 * ```
 *
 * Sorting is exposed via the TopAppBar overflow (`Sort` icon → DropdownMenu
 * with the four [SortMode]s). Search is a plain [OutlinedTextField] below the
 * bar; the query is forwarded to the ViewModel and the grid reacts in real
 * time (filter is applied inside [LibraryViewModel.items]).
 *
 * The card itself owns its long-press context menu (see [LibraryCard]); this
 * screen wires the menu's "Eliminar" action to an [AlertDialog] confirmation
 * before calling [LibraryViewModel.delete].
 *
 * Empty state: an [EmptyState] composable with the `Movie` icon, the Spanish
 * title "Tu biblioteca está vacía" and the helper message "Las películas que
 * descargues aparecerán aquí." It is wrapped in a vertically-scrollable Box so
 * the pull-to-refresh gesture still triggers even with no grid content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val items by viewModel.items.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Pending delete confirmation. When non-null, an AlertDialog is shown
    // asking the user to confirm deleting this item.
    var pendingDelete by remember { mutableStateOf<LibraryItemEntity?>(null) }

    val pullState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca") },
                actions = {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Ordenar")
                    }
                    SortDropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismiss = { sortMenuExpanded = false },
                        current = sortMode,
                        onSelected = {
                            viewModel.setSortMode(it)
                            sortMenuExpanded = false
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            // ---- Search bar (sticky above the grid) ----------------
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Buscar") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                )
            )

            // ---- Grid / empty state with pull-to-refresh ------------
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                state = pullState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.Movie,
                            title = "Tu biblioteca está vacía",
                            message = "Las películas que descargues aparecerán aquí.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 12.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = items,
                            key = { it.path }
                        ) { item ->
                            LibraryCard(
                                item = item,
                                onPlay = { viewModel.play(context, item) },
                                onToggleWatched = { viewModel.toggleWatched(item) },
                                onDelete = { pendingDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Delete confirmation dialog -------------------------------
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar video") },
            text = {
                Text(
                    "¿Seguro que quieres eliminar \"${target.name}\"? " +
                        "El archivo se borrará del dispositivo."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(target)
                        pendingDelete = null
                    }
                ) {
                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------
// Sort dropdown
// -----------------------------------------------------------------------

/**
 * Overflow dropdown menu listing all [SortMode]s. The currently-selected mode
 * is decorated with a "✓" prefix so the user can see at a glance which sort
 * is active.
 */
@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    current: SortMode,
    onSelected: (SortMode) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        SortMode.entries.forEach { mode ->
            val label = sortLabel(mode)
            val display = if (mode == current) "✓ $label" else label
            DropdownMenuItem(
                text = { Text(display) },
                onClick = { onSelected(mode) }
            )
        }
    }
}

/** Human-readable (Spanish) labels for each sort mode. */
private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.DATE_ADDED -> "Fecha de adición"
    SortMode.NAME -> "Nombre"
    SortMode.SIZE -> "Tamaño"
    SortMode.LAST_PLAYED -> "Última reproducción"
}
