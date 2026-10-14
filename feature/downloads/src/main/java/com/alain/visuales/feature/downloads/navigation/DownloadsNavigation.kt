package com.alain.visuales.feature.downloads.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.alain.visuales.feature.downloads.screens.DownloadsScreen

const val DOWNLOADS_GRAPH = "downloads-graph"
const val DOWNLOADS_ROUTE = "downloads"

fun NavGraphBuilder.downloadsGraph(navController: NavHostController) {
    navigation(startDestination = DOWNLOADS_ROUTE, route = DOWNLOADS_GRAPH) {
        composable(DOWNLOADS_ROUTE) {
            DownloadsScreen(modifier = Modifier)
        }
    }
}
