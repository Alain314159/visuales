package com.alain.visuales.feature.downloads.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Film
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alain.visuales.core.common.formatBytes
import com.alain.visuales.core.common.formatEta
import com.alain.visuales.core.common.formatSpeed
import com.alain.visuales.core.data.db.entity.DownloadEntity
import com.alain.visuales.core.data.db.entity.DownloadStatus

/**
 * Material 3 card that represents a single download entry in the queue.
 *
 * Layout (top → bottom):
 *
 * ```
 * ┌──────────────────────────────────────────────────────────────┐
 * │ [status-icon]  Filename (single line, ellipsis)              │
 * │                                                                │
 * │ ▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░  42 %                              │
 * │ (only for DOWNLOADING / PAUSED / QUEUED)                       │
 * │                                                                │
 * │ 1.2 GB / 4.5 GB · 3.4 MB/s · 4m 12s                           │
 * │                                                                │
 * │ [Pause] [Cancel]                       [Reproducir ▶]        │
 * └──────────────────────────────────────────────────────────────┘
 * ```
 *
 * The action buttons shown depend on [DownloadEntity.status]:
 *
 * | Status       | Action buttons                                            |
 * |--------------|------------------------------------------------------------|
 * | DOWNLOADING  | Pausar · Cancelar                                          |
 * | PAUSED       | Reanudar · Cancelar                                        |
 * | QUEUED       | Cancelar                                                   |
 * | COMPLETED    | Reproducir (with `Film` icon) · Eliminar archivo           |
 * | FAILED       | Reintentar · Eliminar                                      |
 * | CANCELLED    | Eliminar                                                   |
 *
 * @param dl                the download entry to render.
 * @param speedBytesPerSec  instantaneous download speed (bytes/s). Only shown
 *                          for DOWNLOADING items; pass 0 to omit.
 * @param onPause           invoked when the user taps "Pausar".
 * @param onResume          invoked when the user taps "Reanudar" / "Reintentar".
 * @param onCancel          invoked when the user taps "Cancelar".
 * @param onPlay            invoked when the user taps "Reproducir".
 * @param onDeleteFile      invoked when the user taps "Eliminar archivo".
 * @param onRemove          invoked when the user taps "Eliminar" (entity-only).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadCard(
    dl: DownloadEntity,
    modifier: Modifier = Modifier,
    speedBytesPerSec: Long = 0L,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onCancel: () -> Unit = {},
    onPlay: () -> Unit = {},
    onDeleteFile: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    val status = remember(dl.id, dl.status) {
        runCatching { DownloadStatus.valueOf(dl.status) }.getOrNull()
            ?: DownloadStatus.QUEUED
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ---- Header row: status icon + filename -------------------

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIcon(status = status, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = dl.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // ---- Progress bar (only for active states) ----------------

            AnimatedVisibility(
                visible = status == DownloadStatus.DOWNLOADING ||
                    status == DownloadStatus.PAUSED ||
                    status == DownloadStatus.QUEUED,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    LinearProgressIndicator(
                        progress = { (dl.progress.coerceIn(0, 100) / 100f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ---- Stats row: size / % / speed / ETA --------------------

            StatsRow(
                dl = dl,
                status = status,
                speedBytesPerSec = speedBytesPerSec
            )

            // ---- Error message (FAILED only) --------------------------

            if (status == DownloadStatus.FAILED && !dl.error.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = dl.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // ---- Actions row ------------------------------------------

            ActionsRow(
                status = status,
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel,
                onPlay = onPlay,
                onDeleteFile = onDeleteFile,
                onRemove = onRemove
            )
        }
    }
}

// -----------------------------------------------------------------------
// Sub-components
// -----------------------------------------------------------------------

@Composable
private fun StatusIcon(status: DownloadStatus, modifier: Modifier = Modifier) {
    when (status) {
        DownloadStatus.DOWNLOADING -> {
            // Animated spinner tinted with primary.
            CircularProgressIndicator(
                modifier = modifier,
                strokeWidth = 2.5.dp,
                strokeCap = StrokeCap.Round
            )
        }
        DownloadStatus.PAUSED -> {
            Icon(
                imageVector = Icons.Filled.Pause,
                contentDescription = "Pausada",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = modifier
            )
        }
        DownloadStatus.QUEUED -> {
            Icon(
                imageVector = Icons.Outlined.HourglassEmpty,
                contentDescription = "En cola",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier
            )
        }
        DownloadStatus.COMPLETED -> {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Completada",
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
        DownloadStatus.FAILED -> {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Fallida",
                tint = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
        }
        DownloadStatus.CANCELLED -> {
            Icon(
                imageVector = Icons.Outlined.Cancel,
                contentDescription = "Cancelada",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun StatsRow(
    dl: DownloadEntity,
    status: DownloadStatus,
    speedBytesPerSec: Long
) {
    val parts = mutableListOf<String>()

    if (dl.size > 0) {
        parts += if (dl.bytesDownloaded > 0 && status != DownloadStatus.COMPLETED) {
            "${formatBytes(dl.bytesDownloaded)} / ${formatBytes(dl.size)}"
        } else {
            formatBytes(dl.size)
        }
    } else if (dl.bytesDownloaded > 0) {
        parts += formatBytes(dl.bytesDownloaded)
    }

    if (status == DownloadStatus.DOWNLOADING ||
        status == DownloadStatus.PAUSED ||
        status == DownloadStatus.QUEUED
    ) {
        parts += "${dl.progress.coerceIn(0, 100)}%"
    }

    if (status == DownloadStatus.DOWNLOADING && speedBytesPerSec > 0) {
        parts += formatSpeed(speedBytesPerSec)
        // ETA = (remaining bytes / speed) * 1000  ms
        if (dl.size > 0 && dl.bytesDownloaded < dl.size) {
            val remaining = dl.size - dl.bytesDownloaded
            val etaMs = (remaining * 1000L) / speedBytesPerSec
            parts += formatEta(etaMs)
        }
    }

    if (parts.isEmpty()) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = parts.joinToString("  ·  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionsRow(
    status: DownloadStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onPlay: () -> Unit,
    onDeleteFile: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left cluster: status-specific actions.
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                DownloadStatus.DOWNLOADING -> {
                    IconActionButton(
                        icon = Icons.Filled.Pause,
                        description = "Pausar",
                        onClick = onPause
                    )
                    IconActionButton(
                        icon = Icons.Outlined.Cancel,
                        description = "Cancelar",
                        onClick = onCancel
                    )
                }
                DownloadStatus.PAUSED -> {
                    IconActionButton(
                        icon = Icons.Filled.PlayArrow,
                        description = "Reanudar",
                        onClick = onResume
                    )
                    IconActionButton(
                        icon = Icons.Outlined.Cancel,
                        description = "Cancelar",
                        onClick = onCancel
                    )
                }
                DownloadStatus.QUEUED -> {
                    IconActionButton(
                        icon = Icons.Outlined.Cancel,
                        description = "Cancelar",
                        onClick = onCancel
                    )
                }
                DownloadStatus.COMPLETED -> {
                    FilledTonalButton(onClick = onPlay) {
                        Icon(
                            imageVector = Icons.Filled.Film,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Reproducir")
                    }
                    Spacer(Modifier.width(8.dp))
                    IconActionButton(
                        icon = Icons.Filled.Delete,
                        description = "Eliminar archivo",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onDeleteFile
                    )
                }
                DownloadStatus.FAILED -> {
                    IconActionButton(
                        icon = Icons.Filled.Refresh,
                        description = "Reintentar",
                        onClick = onResume
                    )
                    IconActionButton(
                        icon = Icons.Filled.Delete,
                        description = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onRemove
                    )
                }
                DownloadStatus.CANCELLED -> {
                    IconActionButton(
                        icon = Icons.Filled.Delete,
                        description = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onRemove
                    )
                }
            }
        }
    }
}

@Composable
private fun IconActionButton(
    icon: ImageVector,
    description: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint
        )
    }
}
