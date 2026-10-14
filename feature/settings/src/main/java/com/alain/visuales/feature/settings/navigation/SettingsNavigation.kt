package com.alain.visuales.feature.settings.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.alain.visuales.feature.settings.screens.SettingsScreen

const val SETTINGS_GRAPH = "settings-graph"
const val SETTINGS_ROUTE = "settings"

fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    navigation(startDestination = SETTINGS_ROUTE, route = SETTINGS_GRAPH) {
        composable(SETTINGS_ROUTE) {
            SettingsScreen(modifier = Modifier)
        }
    }
}
