package com.alain.visuales.feature.catalog.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alain.visuales.core.common.CatalogSections
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.designsystem.components.EmptyState
import com.alain.visuales.feature.catalog.components.HeroCard
import com.alain.visuales.feature.catalog.components.SectionCarousel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen of the catalog tab.
 *
 * Layout (LazyColumn):
 *  1. [HeroCard] — most recently added directory across all sections.
 *  2. One [SectionCarousel] per [CatalogSections.FEATURED] entry that has items.
 *  3. A small "last sync" footer.
 *
 * Pull-to-refresh triggers [CatalogViewModel.triggerHomeRefresh] which clears
 * the expired Layer-2 cache and re-scrapes the top-level sections' listings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDirectoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val homeSections by viewModel.homeSections.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    val hero = recentlyAdded.firstOrNull()
    val hasContent = homeSections.values.any { it.isNotEmpty() } || hero != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Visuales") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { viewModel.triggerHomeRefresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.triggerHomeRefresh() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!hasContent && !isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Outlined.Movie,
                        title = "Sin contenido",
                        message = "Tira hacia abajo para sincronizar el catálogo o espera a que termine la sincronización inicial."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (hero != null) {
                        item(key = "hero") {
                            HeroCard(directory = hero, onClick = { onDirectoryClick(hero.path) })
                        }
                    }
                    CatalogSections.FEATURED.forEach { section ->
                        val items = homeSections[section].orEmpty()
                        if (items.isNotEmpty()) {
                            item(key = "section-$section") {
                                SectionCarousel(
                                    title = section,
                                    items = items,
                                    onItemClick = { dir -> onDirectoryClick(dir.path) }
                                )
                            }
                        }
                    }
                    val lastSync = syncState?.lastSyncAt
                    if (lastSync != null && lastSync > 0) {
                        item(key = "sync-footer") {
                            Text(
                                text = "Última sincronización: ${syncTimeFormatter.format(Date(lastSync))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else if (!hasContent) {
                        item(key = "sync-hint") {
                            Text(
                                text = "Sincronizando catálogo…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

private val syncTimeFormatter: SimpleDateFormat by lazy {
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}
