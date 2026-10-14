package com.alain.visuales.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * Material 3 controls overlay for the player.
 *
 * Renders two bars over the video surface:
 *
 * - **Top bar** — back arrow + video title.
 * - **Bottom bar** — current / total time, seek slider, play/pause, speed
 *   selector (dropdown), subtitle toggle, PiP button.
 *
 * The caller controls visibility with [isVisible] — typically auto-hidden
 * 5 s after the last interaction while playing.
 *
 * Strings are hardcoded in Spanish per project convention.
 *
 * @param isPlaying    Whether the player is currently playing (drives the
 *                     play/pause icon).
 * @param position     Current playback position in ms.
 * @param duration     Total duration in ms (0 if unknown).
 * @param title        Video title shown in the top bar.
 * @param speed        Current playback speed (used to mark the active menu
 *                     item). Defaults to 1.0×.
 * @param subtitlesEnabled Whether subtitles are currently on (drives the
 *                     tinted icon).
 * @param isVisible    Whether the overlay should be drawn.
 * @param onPlayPause  Toggle play/pause.
 * @param onSeek       Seek to an absolute position in ms.
 * @param onSpeedChange Set the playback speed (e.g. 0.5, 1.0, 1.5, 2.0).
 * @param onPip        Enter Picture-in-Picture.
 * @param onBack       Navigate back from the player.
 * @param onToggleSubtitles  Toggle subtitle visibility.
 * @param onToggleAudioTrack Cycle to the next audio track.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    title: String,
    speed: Float = 1.0f,
    subtitlesEnabled: Boolean = false,
    isVisible: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPip: () -> Unit,
    onBack: () -> Unit,
    onToggleSubtitles: () -> Unit = {},
    onToggleAudioTrack: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                // Eat clicks so they don't pass through to the gesture layer
                // underneath. (No-op onClick.)
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
        ) {
            // --- Top bar ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            // --- Center play/pause ---
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            // --- Bottom bar ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Seek slider + time labels
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatTime(position),
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        SeekSlider(
                            position = position,
                            duration = duration,
                            onSeek = onSeek
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                // Action row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White
                        )
                    }

                    SpeedMenu(
                        current = speed,
                        onChange = onSpeedChange
                    )

                    IconButton(onClick = onToggleSubtitles) {
                        Icon(
                            imageVector = Icons.Filled.Subtitles,
                            contentDescription = "Subtítulos",
                            tint = if (subtitlesEnabled) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    IconButton(onClick = onToggleAudioTrack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Pista de audio",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = onPip) {
                        Icon(
                            imageVector = Icons.Filled.PictureInPicture,
                            contentDescription = "Imagen en imagen",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * The seek slider. Sits between the two time labels in the bottom bar.
 *
 * Uses Material 3 [Slider] with a transparent track and a draggable thumb.
 * The slider reports absolute position via [onSeek]; while dragging we cache
 * the local value so the thumb tracks the finger instead of jittering with
 * the player's reported position.
 */
@Composable
private fun SeekSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }

    val safeDuration = max(duration, 1L)
    val currentValue = if (dragging) dragValue else (position.toFloat() / safeDuration).coerceIn(0f, 1f)

    Slider(
        value = currentValue,
        onValueChange = {
            dragging = true
            dragValue = it
        },
        onValueChangeFinished = {
            dragging = false
            onSeek((dragValue * safeDuration).toLong())
        },
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        )
    )
}

/**
 * Speed selector — opens a dropdown of common speeds (0.5× … 2.0×).
 */
@Composable
private fun SpeedMenu(
    current: Float,
    onChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = "Velocidad",
                tint = if (current != 1.0f) MaterialTheme.colorScheme.primary else Color.White
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            speeds.forEach { s ->
                DropdownMenuItem(
                    text = { Text(formatSpeedLabel(s) + if (s == current) "  ✓" else "") },
                    onClick = {
                        onChange(s)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ---- Helpers --------------------------------------------------------------

/** Format a millisecond duration as "h:mm:ss" or "m:ss". */
internal fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> "%d:%02d:%02d".format(h, m, s)
        else -> "%d:%02d".format(m, s)
    }
}

/** Format a playback speed as "1.0×" / "1.5×" etc. */
internal fun formatSpeedLabel(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}.0×"
    } else {
        "$speed×"
    }
}
