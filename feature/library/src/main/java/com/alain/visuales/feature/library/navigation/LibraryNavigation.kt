package com.alain.visuales.feature.library.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.alain.visuales.feature.library.screens.LibraryScreen

const val LIBRARY_GRAPH = "library-graph"
const val LIBRARY_ROUTE = "library"

fun NavGraphBuilder.libraryGraph(navController: NavHostController) {
    navigation(startDestination = LIBRARY_ROUTE, route = LIBRARY_GRAPH) {
        composable(LIBRARY_ROUTE) {
            LibraryScreen(modifier = Modifier)
        }
    }
}
