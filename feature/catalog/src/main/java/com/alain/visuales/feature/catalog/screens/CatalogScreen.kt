package com.alain.visuales.feature.catalog.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.repository.CatalogRepository
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    initialPath: String = "/",
    onDirectoryClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val currentPath by viewModel.currentPath.collectAsState()

    LaunchedEffect(initialPath) {
        if (initialPath != "/") viewModel.navigateTo(initialPath)
    }

    val repo = (viewModel as? Any)?.let { null }  // not used directly; we use Hilt-injected repo
    // We rely on injected repo via the screen-level Hilt injection in next iteration.
    // For now, scaffold basic UI.

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(currentPath) },
                navigationIcon = {
                    if (onBack != null && currentPath != "/") {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        Text(
            text = "Catálogo: $currentPath",
            modifier = Modifier.padding(padding).padding(16.dp)
        )
    }
}
