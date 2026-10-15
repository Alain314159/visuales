package com.alain.visuales.feature.catalog.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alain.visuales.core.common.CatalogSortMode
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.db.entity.DirectoryEntryEntity
import com.alain.visuales.core.designsystem.components.EmptyState
import com.alain.visuales.feature.catalog.components.Breadcrumbs
import com.alain.visuales.feature.catalog.components.Crumb
import com.alain.visuales.feature.catalog.components.EntryRow

/**
 * Browse screen — lists the children (sub-directories + files) of [parentPath].
 *
 * Top bar shows the folder's display name; below it, a [Breadcrumbs] row lets
 * the user jump back to any ancestor. The list mixes:
 *  - directory rows from `currentChildren` (Layer 1, synced tree).
 *  - file rows from `currentEntries` (Layer 2, scraped on demand).
 *
 * When at least one VIDEO entry is present, an "Descargar todos" FAB enqueues
 * the whole batch via [CatalogViewModel.enqueueAllVideos].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    parentPath: String,
    onNavigateToPath: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    // Seed the VM's currentPath whenever nav argument changes.
    LaunchedEffect(parentPath) {
        viewModel.navigateTo(parentPath)
    }

    val currentPath by viewModel.currentPath.collectAsState()
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val children by viewModel.currentChildren.collectAsState()
    val entries by viewModel.currentEntries.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val title = currentDirectory?.name
        ?: currentPath.trim('/').substringAfterLast('/').ifEmpty { "Catálogo" }
    val crumbs = remember(currentPath) { buildCrumbs(currentPath) }
    val hasVideos = entries.any { it.type == "VIDEO" && !it.isDirectory }

    val items: List<BrowseListItem> = remember(children, entries) {
        val dirs = children.map { BrowseListItem.Dir(it) }
        val files = entries.map { BrowseListItem.Entry(it) }
        dirs + files
    }
    val isLoading = currentDirectory == null && items.isEmpty()

    // Trigger Emby lookups for visible directories so posters load lazily.
    LaunchedEffect(children) {
        children.take(30).forEach { dir -> viewModel.requestEmbyLookup(dir.path) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.forceRefresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                        }
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(Icons.Filled.Sort, contentDescription = "Ordenar")
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                CatalogSortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.displayLabel) },
                                        onClick = {
                                            viewModel.setSortMode(mode)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                if (crumbs.size > 1) {
                    Breadcrumbs(
                        crumbs = crumbs,
                        onCrumbClick = { c -> onNavigateToPath(c.path) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (hasVideos) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.enqueueAllVideos(entries) },
                    icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                    text = { Text("Descargar todos") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            items.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.Folder,
                    title = "Carpeta vacía",
                    message = "No se encontraron archivos ni subcarpetas en esta ubicación.",
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        when (item) {
                            is BrowseListItem.Dir -> EntryRow(
                                directory = item.directory,
                                onClick = { onNavigateToPath(item.directory.path) }
                            )
                            is BrowseListItem.Entry -> EntryRow(
                                entry = item.entry,
                                onClick = { viewModel.enqueueDownload(item.entry) },
                                onDownloadClick = { viewModel.enqueueDownload(item.entry) }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

/** Sealed wrapper so we can mix directories and file entries in one LazyColumn. */
private sealed interface BrowseListItem {
    val id: String
    data class Dir(val directory: DirectoryEntity) : BrowseListItem {
        override val id: String get() = "d:${directory.path}"
    }
    data class Entry(val entry: DirectoryEntryEntity) : BrowseListItem {
        override val id: String get() = "e:${entry.id}"
    }
}

/** Build the breadcrumb trail (root + every path segment) for [path]. */
private fun buildCrumbs(path: String): List<Crumb> {
    val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
    if (segments.isEmpty()) return listOf(Crumb("Inicio", "/"))
    val crumbs = mutableListOf<Crumb>()
    crumbs.add(Crumb(label = "Inicio", path = "/"))
    var acc = ""
    for (s in segments) {
        acc = "$acc/$s"
        crumbs.add(Crumb(label = s, path = "$acc/"))
    }
    return crumbs
}
