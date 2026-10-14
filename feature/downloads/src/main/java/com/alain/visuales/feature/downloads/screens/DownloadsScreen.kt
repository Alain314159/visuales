package com.alain.visuales.feature.downloads.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pullrefresh.PullToRefreshBox
import androidx.compose.material3.pullrefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alain.visuales.core.data.db.entity.DownloadEntity
import com.alain.visuales.core.designsystem.components.EmptyState
import com.alain.visuales.feature.downloads.components.DownloadCard

/**
 * Top-level screen for the Downloads feature.
 *
 * Three tabs:
 *  - **Activas**: in-progress, queued and paused downloads.
 *  - **Completadas**: finished downloads (tap "Reproducir" to open externally).
 *  - **Fallidas**: downloads that failed and can be retried or removed.
 *
 * Each tab supports pull-to-refresh (visual-only — the underlying flows are
 * already live, but the gesture gives the user a familiar "I want fresh data"
 * affordance). The top app bar exposes a "Limpiar" overflow action that calls
 * [DownloadsViewModel.clearFinished] to wipe all COMPLETED / CANCELLED / FAILED
 * rows from the database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val active by viewModel.active.collectAsStateWithLifecycle()
    val completed by viewModel.completed.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val speeds by viewModel.speeds.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Descargas") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Limpiar") },
                            onClick = {
                                menuExpanded = false
                                viewModel.clearFinished()
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Activas (${active.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Completadas (${completed.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Fallidas (${failed.size})") }
                )
            }

            when (selectedTab) {
                0 -> DownloadsTab(
                    items = active,
                    speeds = speeds,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    emptyIcon = Icons.Filled.Download,
                    emptyTitle = "Aún no hay descargas",
                    emptyMessage = "Ve al catálogo y pulsa en una película o serie.",
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onCancel = viewModel::cancel,
                    onPlay = { viewModel.playVideo(context, it) },
                    onDeleteFile = { viewModel.remove(it, deleteFile = true) },
                    onRemove = { viewModel.remove(it, deleteFile = false) }
                )
                1 -> DownloadsTab(
                    items = completed,
                    speeds = speeds,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    emptyIcon = Icons.Filled.DownloadDone,
                    emptyTitle = "Nada por aquí",
                    emptyMessage = "Cuando termines una descarga aparecerá en esta lista.",
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onCancel = viewModel::cancel,
                    onPlay = { viewModel.playVideo(context, it) },
                    onDeleteFile = { viewModel.remove(it, deleteFile = true) },
                    onRemove = { viewModel.remove(it, deleteFile = false) }
                )
                2 -> DownloadsTab(
                    items = failed,
                    speeds = speeds,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    emptyIcon = Icons.Filled.Error,
                    emptyTitle = "Sin descargas fallidas",
                    emptyMessage = "Si una descarga falla, aparecerá aquí para que puedas reintentarla.",
                    onPause = viewModel::pause,
                    onResume = viewModel::retry,
                    onCancel = viewModel::cancel,
                    onPlay = { viewModel.playVideo(context, it) },
                    onDeleteFile = { viewModel.remove(it, deleteFile = true) },
                    onRemove = { viewModel.remove(it, deleteFile = false) }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------
// Per-tab content
// -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsTab(
    items: List<DownloadEntity>,
    speeds: Map<String, Long>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onPlay: (DownloadEntity) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val pullState = rememberPullToRefreshState()

    if (items.isEmpty()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Wrap the empty state in a vertically-scrollable Box so the
            // pull-to-refresh gesture still triggers even when there's no
            // list content to scroll.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                EmptyState(
                    icon = emptyIcon,
                    title = emptyTitle,
                    message = emptyMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp)
                )
            }
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { dl ->
                DownloadCard(
                    dl = dl,
                    speedBytesPerSec = speeds[dl.id] ?: 0L,
                    onPause = { onPause(dl.id) },
                    onResume = { onResume(dl.id) },
                    onCancel = { onCancel(dl.id) },
                    onPlay = { onPlay(dl) },
                    onDeleteFile = { onDeleteFile(dl.id) },
                    onRemove = { onRemove(dl.id) }
                )
            }
        }
    }
}
