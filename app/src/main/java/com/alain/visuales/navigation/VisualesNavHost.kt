package com.alain.visuales.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.alain.visuales.R
import com.alain.visuales.core.data.preferences.SettingsRepository
import com.alain.visuales.feature.catalog.navigation.catalogGraph
import com.alain.visuales.feature.catalog.navigation.CATALOG_GRAPH
import com.alain.visuales.feature.catalog.navigation.CATALOG_HOME_ROUTE
import com.alain.visuales.feature.catalog.navigation.CATALOG_SEARCH_ROUTE
import com.alain.visuales.feature.downloads.navigation.downloadsGraph
import com.alain.visuales.feature.library.navigation.libraryGraph
import com.alain.visuales.feature.settings.navigation.settingsGraph
import com.alain.visuales.feature.settings.navigation.SETTINGS_ROUTE
import com.alain.visuales.onboarding.OnboardingScreen
import com.alain.visuales.sync.listado.worker.ListadoSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavRootViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean> = settings.settings
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun markOnboardingCompleted(context: android.content.Context) {
        viewModelScope.launch {
            settings.setOnboardingCompleted(true)
            ListadoSyncWorker.runNow(context)
        }
    }
}

/**
 * Root navigation: bottom bar with 4 tabs (Home, Search, Downloads, Library)
 * + Settings reachable from the top bar of any screen.
 *
 * Onboarding is shown first if user hasn't completed it yet.
 */
@Composable
fun VisualesNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val navViewModel: NavRootViewModel = hiltViewModel()
    val onboardingCompleted by navViewModel.onboardingCompleted.collectAsState()

    val showBottomBar = currentDestination?.route?.let { route ->
        TopLevelDestination.entries.any { it.graphRoute == route || route.startsWith("${it.graphRoute}/") }
    } ?: false

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar && onboardingCompleted) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.graphRoute } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(
                                    dest.graphRoute,
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
        val startDest = if (onboardingCompleted) CATALOG_GRAPH else ONBOARDING_ROUTE
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding)
        ) {
            composable(ONBOARDING_ROUTE) {
                OnboardingScreen(
                    onCompleted = {
                        navViewModel.markOnboardingCompleted(androidx.compose.ui.platform.LocalContext.current)
                        navController.navigate(CATALOG_GRAPH) {
                            popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                        }
                    }
                )
            }
            catalogGraph(navController)
            downloadsGraph(navController)
            libraryGraph(navController)
            settingsGraph(navController)
        }
    }
}

const val ONBOARDING_ROUTE = "onboarding"

enum class TopLevelDestination(
    val graphRoute: String,
    val icon: ImageVector,
    val labelRes: Int
) {
    HOME(CATALOG_HOME_ROUTE, Icons.Outlined.Movie, R.string.nav_home),
    SEARCH(CATALOG_SEARCH_ROUTE, Icons.Outlined.Search, R.string.nav_search),
    DOWNLOADS("downloads", Icons.Outlined.Download, R.string.nav_downloads),
    LIBRARY("library", Icons.Outlined.VideoLibrary, R.string.nav_library);
}

