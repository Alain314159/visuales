package com.alain.visuales.feature.settings.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alain.visuales.core.data.preferences.ThemeMode
import com.alain.visuales.feature.settings.components.ClickableRow
import com.alain.visuales.feature.settings.components.ColorPickerRow
import com.alain.visuales.feature.settings.components.SegmentedRow
import com.alain.visuales.feature.settings.components.SettingsCard
import com.alain.visuales.feature.settings.components.SettingsHeader
import com.alain.visuales.feature.settings.components.SliderRow
import com.alain.visuales.feature.settings.components.ToggleRow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val SYNC_INTERVAL_OPTIONS = longArrayOf(6L, 12L, 24L, 48L, 72L)

private val SUBTITLE_TEXT_PRESETS = listOf(
    0xFFFFFFFF.toInt(),
    0xFF000000.toInt(),
    0xFFFFEB3B.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFFF5722.toInt(),
    0xFF00E676.toInt()
)

private val SUBTITLE_BG_PRESETS = listOf(
    0xAA000000.toInt(),
    0xFF000000.toInt(),
    0x66000000.toInt(),
    0x00000000.toInt(),
    0xAA1A237E.toInt(),
    0xAA4E342E.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // --- Folder picker (OpenDocumentTree) ---
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist read+write permission so we can use it across app restarts.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not allow persistable permissions; proceed anyway.
            }
            viewModel.setDownloadFolderUri(uri.toString())
        }
    }

    // --- Dialog state ---
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ---------- 1. General ----------
            item {
                SettingsHeader("General")
                SettingsCard {
                    ClickableRow(
                        title = "Idioma",
                        subtitle = "Español (Cuba)",
                        onClick = { /* no-op */ }
                    )
                    SegmentedRow(
                        title = "Tema",
                        subtitle = "Modo claro / oscuro / automático",
                        options = listOf("Sistema", "Claro", "Oscuro"),
                        selected = settings.themeMode.ordinal,
                        onSelect = { idx ->
                            val mode = ThemeMode.entries[idx]
                            viewModel.setThemeMode(mode)
                        }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ToggleRow(
                            title = "Color dinámico",
                            subtitle = "Material You (Android 12+)",
                            checked = settings.dynamicColor,
                            onChange = viewModel::setDynamicColor,
                            icon = Icons.Outlined.Palette
                        )
                    }
                    OutlinedTextField(
                        value = settings.baseUrl,
                        onValueChange = viewModel::setBaseUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        label = { Text("URL del servidor") },
                        placeholder = { Text("https://visuales.uclv.cu") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }
            }

            // ---------- 2. Catálogo ----------
            item {
                SettingsHeader("Catálogo")
                SettingsCard {
                    SliderRow(
                        title = "Intervalo de sincronización",
                        subtitle = "Cada cuántas horas se actualiza el listado",
                        value = SYNC_INTERVAL_OPTIONS.indexOf(settings.syncIntervalHours)
                            .coerceAtLeast(0).toFloat(),
                        valueRange = 0f..(SYNC_INTERVAL_OPTIONS.size - 1).toFloat(),
                        steps = SYNC_INTERVAL_OPTIONS.size - 2,
                        onValueChange = { idx ->
                            val snapped = idx.toInt().coerceIn(
                                0,
                                SYNC_INTERVAL_OPTIONS.size - 1
                            )
                            viewModel.setSyncIntervalHours(SYNC_INTERVAL_OPTIONS[snapped])
                        },
                        valueFormatter = { _ ->
                            "${settings.syncIntervalHours} h"
                        }
                    )
                    ToggleRow(
                        title = "Verificación profunda",
                        subtitle = "Usa el scraper si algo no está en el listado",
                        checked = settings.deepScraping,
                        onChange = viewModel::setDeepScraping,
                        icon = Icons.Outlined.CloudSync
                    )
                    ToggleRow(
                        title = "Metadatos enriquecidos",
                        subtitle = "Requiere internet fuera de la red UCLV",
                        checked = settings.enrichedMetadata,
                        onChange = viewModel::setEnrichedMetadata,
                        icon = Icons.Outlined.CloudSync
                    )
                    ToggleRow(
                        title = "Usar Emby para metadatos",
                        subtitle = "Portadas, sinopsis y géneros desde emby.uclv.cu",
                        checked = settings.embyEnabled,
                        onChange = viewModel::setEmbyEnabled,
                        icon = Icons.Outlined.VideoLibrary
                    )
                    ClickableRow(
                        title = "Última sincronización",
                        subtitle = formatSyncTimestamp(settings.lastSyncShownAt),
                        onClick = { /* no-op */ }
                    )
                }
            }

            // ---------- 3. Descargas ----------
            item {
                SettingsHeader("Descargas")
                SettingsCard {
                    ClickableRow(
                        title = "Carpeta de descargas",
                        subtitle = settings.downloadFolderUri
                            ?: "Por defecto (Downloads/Visuales)",
                        icon = Icons.Outlined.Folder,
                        onClick = { folderPickerLauncher.launch(null) },
                        trailing = {
                            TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                                Text("Elegir carpeta")
                            }
                        }
                    )
                    SliderRow(
                        title = "Conexiones por archivo",
                        value = settings.connectionsPerFile.toFloat(),
                        valueRange = 1f..32f,
                        steps = 30,
                        onValueChange = { viewModel.setConnectionsPerFile(it.toInt()) },
                        valueFormatter = { v -> "${v.toInt()}" }
                    )
                    SliderRow(
                        title = "Descargas simultáneas",
                        value = settings.maxConcurrentDownloads.toFloat(),
                        valueRange = 1f..5f,
                        steps = 3,
                        onValueChange = { viewModel.setMaxConcurrentDownloads(it.toInt()) },
                        valueFormatter = { v -> "${v.toInt()}" }
                    )
                    ToggleRow(
                        title = "Solo WiFi",
                        checked = settings.wifiOnly,
                        onChange = viewModel::setWifiOnly
                    )
                    ToggleRow(
                        title = "Solo enchufado",
                        checked = settings.chargingOnly,
                        onChange = viewModel::setChargingOnly
                    )
                    ClickableRow(
                        title = "Horario programado",
                        subtitle = formatScheduledWindow(
                            settings.scheduledDownloadStartHour,
                            settings.scheduledDownloadEndHour
                        ),
                        icon = Icons.Outlined.Schedule,
                        onClick = { showStartPicker = true }
                    )
                    SliderRow(
                        title = "Espacio mínimo libre",
                        value = settings.minFreeSpaceMb.toFloat(),
                        valueRange = 100f..5000f,
                        steps = 48,
                        onValueChange = { viewModel.setMinFreeSpaceMb(it.toLong()) },
                        valueFormatter = { v -> "${v.toInt()} MB" }
                    )
                }
            }

            // ---------- 4. Subtítulos ----------
            item {
                SettingsHeader("Subtítulos")
                SettingsCard {
                    ToggleRow(
                        title = "Auto-descargar subtítulos",
                        checked = settings.autoSubtitles,
                        onChange = viewModel::setAutoSubtitles,
                        icon = Icons.Outlined.Subtitles
                    )
                    SliderRow(
                        title = "Tamaño de fuente",
                        value = settings.subtitleFontSize.toFloat(),
                        valueRange = 8f..48f,
                        steps = 39,
                        onValueChange = { viewModel.setSubtitleFontSize(it.toInt()) },
                        valueFormatter = { v -> "${v.toInt()} sp" }
                    )
                    ColorPickerRow(
                        title = "Color del texto",
                        currentColor = settings.subtitleColor,
                        onColorSelected = viewModel::setSubtitleColor,
                        presetColors = SUBTITLE_TEXT_PRESETS
                    )
                    ColorPickerRow(
                        title = "Color del fondo",
                        currentColor = settings.subtitleBackgroundColor,
                        onColorSelected = viewModel::setSubtitleBackgroundColor,
                        presetColors = SUBTITLE_BG_PRESETS
                    )
                    // Preview row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(CircleShape)
                            .background(Color(settings.subtitleBackgroundColor))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Subtítulos de muestra",
                            color = Color(settings.subtitleColor),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ---------- 5. Reproductor ----------
            item {
                SettingsHeader("Reproductor")
                SettingsCard {
                    ToggleRow(
                        title = "Gestos activados",
                        subtitle = "Brillo, volumen y búsqueda por deslizamiento",
                        checked = settings.gesturesEnabled,
                        onChange = viewModel::setGesturesEnabled,
                        icon = Icons.Outlined.VideoLibrary
                    )
                    ToggleRow(
                        title = "PiP automático",
                        subtitle = "Al salir de la app, sigue reproduciendo en PiP",
                        checked = settings.pipAuto,
                        onChange = viewModel::setPipAuto
                    )
                    ToggleRow(
                        title = "Reanudar reproducción",
                        subtitle = "Continúa desde donde lo dejaste",
                        checked = settings.resumePlayback,
                        onChange = viewModel::setResumePlayback
                    )
                    SliderRow(
                        title = "Velocidad por defecto",
                        value = settings.defaultPlaybackSpeed,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        onValueChange = { viewModel.setDefaultPlaybackSpeed(it) },
                        valueFormatter = { v -> String.format("%.1fx", v) }
                    )
                }
            }

            // ---------- 6. Notificaciones ----------
            item {
                SettingsHeader("Notificaciones")
                SettingsCard {
                    ToggleRow(
                        title = "Descargas activas",
                        checked = settings.notifDownloadsActive,
                        onChange = viewModel::setNotifDownloadsActive,
                        icon = Icons.Outlined.Notifications
                    )
                    ToggleRow(
                        title = "Descargas completadas",
                        checked = settings.notifDownloadsCompleted,
                        onChange = viewModel::setNotifDownloadsCompleted
                    )
                    ToggleRow(
                        title = "Descargas fallidas",
                        checked = settings.notifDownloadsFailed,
                        onChange = viewModel::setNotifDownloadsFailed
                    )
                    ToggleRow(
                        title = "Novedades del catálogo",
                        checked = settings.notifCatalogUpdates,
                        onChange = viewModel::setNotifCatalogUpdates
                    )
                    ToggleRow(
                        title = "Actualizaciones de la app",
                        checked = settings.notifAppUpdates,
                        onChange = viewModel::setNotifAppUpdates
                    )
                }
            }

            // ---------- 7. Acerca de ----------
            item {
                SettingsHeader("Acerca de")
                SettingsCard {
                    ClickableRow(
                        title = "Versión actual",
                        subtitle = "v2.0.0",
                        icon = Icons.Outlined.Info,
                        onClick = { /* no-op */ }
                    )
                    ClickableRow(
                        title = "Buscar actualizaciones",
                        icon = Icons.Outlined.Tune,
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Buscando...")
                            }
                        }
                    )
                    ClickableRow(
                        title = "Código fuente",
                        subtitle = "github.com/Alain314159/visuales",
                        icon = Icons.Outlined.Code,
                        onClick = {
                            uriHandler.openUri("https://github.com/Alain314159/visuales")
                        },
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    ClickableRow(
                        title = "Reportar un bug",
                        subtitle = "Abrir un issue en GitHub",
                        icon = Icons.Outlined.BugReport,
                        onClick = {
                            uriHandler.openUri(
                                "https://github.com/Alain314159/visuales/issues/new"
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    ClickableRow(
                        title = "Licencia MIT",
                        subtitle = "Copyright © 2025 Alain314159",
                        onClick = {
                            uriHandler.openUri(
                                "https://github.com/Alain314159/visuales/blob/main/LICENSE"
                            )
                        }
                    )
                }
            }

            // ---------- 8. Zona peligrosa ----------
            item {
                SettingsHeader("Zona peligrosa")
                SettingsCard {
                    ClickableRow(
                        title = "Borrar caché del catálogo",
                        subtitle = "Elimina el listado en caché. Se descargará de nuevo en la próxima sincronización.",
                        icon = Icons.Outlined.CleaningServices,
                        onClick = {
                            confirmAction = {
                                viewModel.clearCatalogCache()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Caché del catálogo borrada")
                                }
                            }
                        }
                    )
                    ClickableRow(
                        title = "Borrar todas las descargas",
                        subtitle = "Elimina el registro de descargas (no borra los archivos en disco).",
                        icon = Icons.Outlined.DeleteForever,
                        onClick = {
                            confirmAction = {
                                viewModel.clearAllDownloads()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Descargas borradas")
                                }
                            }
                        }
                    )
                    ClickableRow(
                        title = "Restablecer ajustes",
                        subtitle = "Vuelve todos los valores a sus predeterminados.",
                        icon = Icons.Outlined.RestartAlt,
                        onClick = {
                            confirmAction = {
                                viewModel.resetSettings()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Ajustes restablecidos")
                                }
                            }
                        }
                    )
                }
            }

            // Bottom breathing room
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // --- Confirmation dialog for destructive actions ---
    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("¿Estás seguro?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        action()
                        confirmAction = null
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Start time picker ---
    if (showStartPicker) {
        val state = rememberTimePickerState(
            initialHour = if (settings.scheduledDownloadStartHour in 0..23)
                settings.scheduledDownloadStartHour else 0,
            initialMinute = 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            title = { Text("Hora de inicio") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val endHour = settings.scheduledDownloadEndHour
                    viewModel.setScheduledDownload(state.hour, endHour)
                    showStartPicker = false
                    // Open the end picker right after.
                    showEndPicker = true
                }) { Text("Siguiente") }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Disable scheduling entirely.
                    viewModel.setScheduledDownload(-1, -1)
                    showStartPicker = false
                }) { Text("Desactivar") }
            }
        )
    }

    // --- End time picker ---
    if (showEndPicker) {
        val state = rememberTimePickerState(
            initialHour = if (settings.scheduledDownloadEndHour in 0..23)
                settings.scheduledDownloadEndHour else 6,
            initialMinute = 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            title = { Text("Hora de fin") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setScheduledDownload(
                        settings.scheduledDownloadStartHour,
                        state.hour
                    )
                    showEndPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ----- Helpers -----

private fun formatSyncTimestamp(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "Nunca"
    val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    return df.format(Date(timestampMillis))
}

private fun formatScheduledWindow(startHour: Int, endHour: Int): String {
    return if (startHour < 0 || endHour < 0) {
        "Desactivado"
    } else {
        "%02d:00 – %02d:00".format(startHour, endHour)
    }
}
