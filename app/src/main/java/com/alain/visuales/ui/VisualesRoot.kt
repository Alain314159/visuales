package com.alain.visuales.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.alain.visuales.navigation.VisualesNavHost
import com.alain.visuales.ui.theme.VisualesTheme

/**
 * Root composable: theme + scaffold + nav host.
 */
@Composable
fun VisualesRoot() {
    VisualesTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            VisualesNavHost(
                modifier = Modifier.padding(padding)
            )
        }
    }
}
