package com.alain.visuales.feature.catalog.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * CatalogScreen is deprecated as of the v1 catalog implementation.
 *
 * The catalog tab now consists of three independent composables routed by
 * [com.alain.visuales.feature.catalog.navigation.catalogGraph]:
 *  - [HomeScreen]
 *  - [BrowseScreen]
 *  - [SearchScreen]
 *
 * This stub is kept so legacy imports don't break callers. New code should
 * invoke the per-route composables directly via the navigation graph.
 */
@Deprecated("Use HomeScreen / BrowseScreen / SearchScreen via catalogGraph instead.")
@Composable
fun CatalogScreen(modifier: Modifier = Modifier) {
    HomeScreen(
        modifier = modifier,
        onDirectoryClick = {},
        onSearchClick = {},
        onSettingsClick = {}
    )
}
