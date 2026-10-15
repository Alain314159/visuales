package com.alain.visuales.feature.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alain.visuales.core.common.formatBytes
import com.alain.visuales.core.data.db.entity.LibraryItemEntity

/**
 * Material 3 card representing a single video in the user's local library.
 *
 * Layout (top → bottom):
 *
 * ```
 * ┌────────────────────────────────────┐
 * │                                    │
 * │     [16:9 thumbnail area]          │  ← placeholder: Film icon on
 * │                          [✓ badge] │     surfaceVariant (top-right
 * │                                    │     "watched" badge if isWatched)
 * │                                    │
 * ├────────────────────────────────────┤
 * │ Filename (2 lines, ellipsis)       │
 * │ 1.4 GB · 1h 32m                    │
 * │ ▓▓▓▓▓▓▓▓░░░░░░░░░░  progress bar   │  ← only if durationMs > 0
 * └────────────────────────────────────┘
 * ```
 *
 * Tap → [onPlay]. Long-press → opens a small [DropdownMenu] anchored at the
 * card with two items:
 *  - "Marcar como visto" / "Marcar como no visto"  → [onToggleWatched]
 *  - "Eliminar"                                    → [onDelete]
 *
 * The long-press also fires [onLongPress] (default no-op) so the screen can
 * react (e.g. show a snackbar) if desired.
 *
 * @param item            the library entry to render.
 * @param onPlay          invoked when the user taps the card.
 * @param onLongPress     invoked after the long-press is detected, in addition
 *                        to opening the local dropdown menu.
 * @param onToggleWatched invoked when the user picks the watched toggle.
 * @param onDelete        invoked when the user picks "Eliminar".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryCard(
    item: LibraryItemEntity,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onToggleWatched: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = {
                    menuExpanded = true
                    onLongPress()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // ---- Thumbnail (16:9 placeholder) --------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )

                if (item.isWatched) {
                    WatchedBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            // ---- Text area --------------------------------------------
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                val meta = buildMetaString(item)
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // ---- Watch progress bar -------------------------------
                val progress = watchProgressFraction(item)
                if (progress != null) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // ---- Long-press context menu ----------------------------------
        // Anchored at the card's coordinate space. Material 3 places it
        // relative to the layout bounds of the parent Box.
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (item.isWatched) "Marcar como no visto"
                        else "Marcar como visto"
                    )
                },
                onClick = {
                    menuExpanded = false
                    onToggleWatched()
                }
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}

// -----------------------------------------------------------------------
// Sub-components
// -----------------------------------------------------------------------

/**
 * Small circular "watched" badge: a green-ish (primary) [Icons.Filled.CheckCircle]
 * sitting on a semi-opaque white scrim so it stays legible over arbitrary
 * thumbnail colors.
 */
@Composable
private fun WatchedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Visto",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -----------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------

/**
 * Build the meta row string: `<size> · <duration>`.
 *
 * Examples:
 *  - "1.4 GB · 1h 32m"
 *  - "1.4 GB"               (no duration)
 *  - "1h 32m"               (no size)
 *  - ""                      (neither — caller should hide the row)
 */
private fun buildMetaString(item: LibraryItemEntity): String {
    val parts = mutableListOf<String>()
    if (item.size > 0) parts += formatBytes(item.size)
    val dur = formatDuration(item.durationMs)
    if (dur.isNotEmpty()) parts += dur
    return parts.joinToString(" · ")
}

/**
 * Format a duration in ms as:
 *  - `Xh Ym` if ≥ 1 hour
 *  - `Xm`    if ≥ 1 minute
 *  - `Xs`    otherwise
 *  - `""`    if null / non-positive
 *
 * We deliberately drop the seconds component when the video is at least a
 * minute long, because durations on a grid card don't need that resolution.
 */
private fun formatDuration(ms: Long?): String {
    if (ms == null || ms <= 0) return ""
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${s}s"
    }
}

/**
 * Watched-progress as a 0..1 fraction. Returns `null` when the bar should not
 * be shown (no duration, or no progress yet).
 */
private fun watchProgressFraction(item: LibraryItemEntity): Float? {
    val dur = item.durationMs ?: return null
    if (dur <= 0 || item.watchProgressMs <= 0) return null
    return item.watchProgressMs.toFloat() / dur.toFloat()
}
