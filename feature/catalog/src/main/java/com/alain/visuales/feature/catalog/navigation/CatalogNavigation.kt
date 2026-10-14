package com.alain.visuales.feature.catalog.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.alain.visuales.feature.catalog.screens.CatalogScreen

const val CATALOG_GRAPH = "catalog-graph"
const val CATALOG_ROUTE = "catalog"
const val CATALOG_DETAIL_ROUTE = "catalog/detail"

fun NavGraphBuilder.catalogGraph(navController: NavHostController) {
    navigation(startDestination = CATALOG_ROUTE, route = CATALOG_GRAPH) {
        composable(CATALOG_ROUTE) {
            CatalogScreen(
                modifier = Modifier,
                onDirectoryClick = { dirPath ->
                    navController.navigate("$CATALOG_DETAIL_ROUTE/$dirPath")
                }
            )
        }
        composable("$CATALOG_DETAIL_ROUTE/{path}") { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path") ?: "/"
            CatalogScreen(
                modifier = Modifier,
                initialPath = path,
                onDirectoryClick = { newPath ->
                    navController.navigate("$CATALOG_DETAIL_ROUTE/$newPath")
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
