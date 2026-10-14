package com.alain.visuales.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.alain.visuales.R
import com.alain.visuales.feature.catalog.catalogGraph
import com.alain.visuales.feature.downloads.downloadsGraph
import com.alain.visuales.feature.library.libraryGraph
import com.alain.visuales.feature.settings.settingsGraph
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Settings

/**
 * Root navigation: 4 bottom tabs + nested graphs per feature.
 *
 * Tabs: Inicio (catalog/home), Buscar, Descargas, Biblioteca
 * Settings is reached from the top app bar (not a tab).
 */
@Composable
fun VisualesNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = currentDestination?.route?.let { route ->
        TopLevelDestination.entries.any { it.route.startsWithBase(route) }
    } ?: false

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route.name } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(
                                    dest.route,
                                    navOptions {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                )
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Catalog.graphPattern(),
            modifier = Modifier.padding(padding)
        ) {
            catalogGraph(navController)
            downloadsGraph(navController)
            libraryGraph(navController)
            settingsGraph(navController)
        }
    }
}

private sealed class Route(val name: String) {
    data object Catalog : Route("catalog")
    data object Downloads : Route("downloads")
    data object Library : Route("library")
    data object Settings : Route("settings")

    fun graphPattern(): String = "$name-graph"
}

private fun String.startsWithBase(route: String?): Boolean {
    if (route == null) return false
    return route == this || route.startsWith("${this}-graph") || route.startsWith("$this/")
}

enum class TopLevelDestination(
    val route: Route,
    val icon: ImageVector,
    val labelRes: Int
) {
    HOME(Route.Catalog, Icons.Outlined.Movie, R.string.nav_home),
    SEARCH(Route.Catalog, Icons.Outlined.Search, R.string.nav_search),
    DOWNLOADS(Route.Downloads, Icons.Outlined.VideoLibrary, R.string.nav_downloads),
    LIBRARY(Route.Library, Icons.Outlined.VideoLibrary, R.string.nav_library);

    val fullRoute: String get() = route.graphPattern()
}
