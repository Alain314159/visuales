package com.alain.visuales.feature.catalog.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.navigation
import com.alain.visuales.feature.catalog.screens.BrowseScreen
import com.alain.visuales.feature.catalog.screens.HomeScreen
import com.alain.visuales.feature.catalog.screens.SearchScreen
import java.net.URLDecoder
import java.net.URLEncoder

/** Graph route for the entire catalog tab (used by `VisualesNavHost`). */
const val CATALOG_GRAPH = "catalog-graph"

/** Legacy route name kept for backwards compatibility with `VisualesNavHost`. */
const val CATALOG_ROUTE = "catalog"

const val CATALOG_DETAIL_ROUTE = "catalog/detail"

/** Route for the Home screen (start destination). */
const val CATALOG_HOME_ROUTE = "catalog-home"

/** Route for BrowseScreen — takes a URL-encoded path argument. */
const val CATALOG_BROWSE_ROUTE = "catalog-browse"
private const val CATALOG_BROWSE_PATTERN = "$CATALOG_BROWSE_ROUTE/{path}"

/** Route for SearchScreen. */
const val CATALOG_SEARCH_ROUTE = "catalog-search"

/**
 * Registers the three catalog destinations:
 *  - [CATALOG_HOME_ROUTE] (start)
 *  - [CATALOG_BROWSE_PATTERN] → [BrowseScreen]
 *  - [CATALOG_SEARCH_ROUTE] → [SearchScreen]
 *
 * The directory path argument is URL-encoded so segments with `/` survive
 * intact through the NavController argument parser.
 */
fun NavGraphBuilder.catalogGraph(navController: NavHostController) {
    navigation(
        startDestination = CATALOG_HOME_ROUTE,
        route = CATALOG_GRAPH
    ) {
        composable(CATALOG_HOME_ROUTE) {
            HomeScreen(
                modifier = Modifier,
                onDirectoryClick = { path ->
                    navController.navigate("$CATALOG_BROWSE_ROUTE/${encode(path)}")
                },
                onSearchClick = {
                    navController.navigate(CATALOG_SEARCH_ROUTE)
                },
                onSettingsClick = {
                    navController.navigate("settings-graph")
                }
            )
        }
        composable(
            route = CATALOG_BROWSE_PATTERN,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getString("path") ?: ""
            val parentPath = decode(raw)
            BrowseScreen(
                modifier = Modifier,
                parentPath = parentPath,
                onNavigateToPath = { newPath ->
                    navController.navigate("$CATALOG_BROWSE_ROUTE/${encode(newPath)}")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(CATALOG_SEARCH_ROUTE) {
            SearchScreen(
                modifier = Modifier,
                onDirectoryClick = { path ->
                    navController.navigate("$CATALOG_BROWSE_ROUTE/${encode(path)}")
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun encode(path: String): String =
    URLEncoder.encode(path, "UTF-8")

private fun decode(path: String): String =
    URLDecoder.decode(path, "UTF-8")
