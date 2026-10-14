package com.alain.visuales.feature.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 8.dp)) {
            ListItem(
                headlineContent = { Text("General") },
                supportingContent = { Text("Idioma, tema, URL del servidor") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Catálogo") },
                supportingContent = { Text("Sincronización, verificación profunda, metadatos") },
                leadingContent = { Icon(Icons.Outlined.CloudSync, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Descargas") },
                supportingContent = { Text("Carpeta, conexiones, simultáneas, WiFi") },
                leadingContent = { Icon(Icons.Outlined.Download, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Subtítulos") },
                supportingContent = { Text("Auto-descarga, estilo, OpenSubtitles") },
                leadingContent = { Icon(Icons.Outlined.Subtitles, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Reproductor") },
                supportingContent = { Text("Gestos, PiP, resume, velocidad") },
                leadingContent = { Icon(Icons.Outlined.VideoLibrary, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Notificaciones") },
                supportingContent = { Text("Por canal, sonido, vibración") },
                leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) }
            )
            ListItem(
                headlineContent = { Text("Acerca de") },
                supportingContent = { Text("Versión 2.0.0, código fuente, licencia") },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) }
            )
        }
    }
}
