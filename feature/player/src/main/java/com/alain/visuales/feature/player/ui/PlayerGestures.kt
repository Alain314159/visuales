package com.alain.visuales.feature.player.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Wraps [content] in a [Box] wired with the standard set of video-player
 * gestures:
 *
 * - **Vertical swipe on the left half** → brightness.
 * - **Vertical swipe on the right half** → volume.
 * - **Horizontal swipe** → relative seek (delta reported in ms).
 * - **Double-tap on the left half** → skip 10 s backwards.
 * - **Double-tap on the right half** → skip 10 s forwards.
 * - **Single tap** → toggle controls overlay.
 *
 * All deltas are signed; the screen is responsible for clamping them to the
 * valid range (brightness 0..1, volume 0..max, position 0..duration).
 *
 * ## Tuning
 *
 * The brightness/volume sensitivity is controlled by [verticalSensitivity]
 * (pixels of drag per full range). The seek sensitivity is controlled by
 * [seekMsPerPixel] (ms per px of horizontal drag). Defaults are tuned for
 * a 6" phone screen.
 *
 * @param onBrightnessChange Delta in [-1, 1] added to current brightness.
 * @param onVolumeChange     Delta in [-1, 1] added to current volume (as a
 *                           fraction of max volume).
 * @param onSeek              Signed delta in ms added to current position.
 * @param onDoubleTapSkip    `true` = +10 s forward, `false` = -10 s back.
 * @param onTap              Single tap (typically toggles controls).
 * @param modifier           Applied to the wrapper Box.
 * @param content            The composable to overlay gestures on (usually
 *                           just the video surface or an empty Box).
 */
@Composable
fun PlayerGestureBox(
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onDoubleTapSkip: (Boolean) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    verticalSensitivity: Float = 600f,
    seekMsPerPixel: Float = 100f,
    content: @Composable () -> Unit
) {
    // Per-gesture session state.
    var gestureKind by remember { mutableStateOf(GestureKind.None) }
    var startX by remember { mutableFloatStateOf(0f) }
    var startY by remember { mutableFloatStateOf(0f) }
    var halfWidth by remember { mutableFloatStateOf(0f) }
    var totalSeekDelta by remember { mutableLongStateOf(0L) }
    var totalVertDelta by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { _ -> onTap() },
                    onDoubleTap = { offset ->
                        // Right half → +10 s, left half → -10 s.
                        onDoubleTapSkip(offset.x > halfWidth)
                    }
                )
            }
            .pointerInput(Unit) {
                // detectDragGestures captures multi-touch poorly; use a single
                // pointer per gesture.
                detectDragGestures(
                    onDragStart = { offset ->
                        startX = offset.x
                        startY = offset.y
                        halfWidth = size.width / 2f
                        totalSeekDelta = 0L
                        totalVertDelta = 0f
                        gestureKind = GestureKind.None
                    },
                    onDragEnd = { gestureKind = GestureKind.None },
                    onDragCancel = { gestureKind = GestureKind.None },
                    onDrag = { change, delta ->
                        change.consume()

                        // Determine gesture direction once we have enough signal.
                        if (gestureKind == GestureKind.None) {
                            totalVertDelta += delta.y
                            totalSeekDelta += (delta.x * seekMsPerPixel).toLong()

                            val absX = abs(totalSeekDelta.toFloat())
                            val absY = abs(totalVertDelta)
                            if (absX > 18f || absY > 18f) {
                                gestureKind = if (absX > absY) GestureKind.Seek
                                else if (startX < halfWidth) GestureKind.Brightness
                                else GestureKind.Volume
                            }
                        }

                        when (gestureKind) {
                            GestureKind.Seek -> {
                                val dx = delta.x
                                totalSeekDelta += (dx * seekMsPerPixel).toLong()
                                if (abs(dx) > 0.5f) {
                                    onSeek((dx * seekMsPerPixel).toLong())
                                }
                            }
                            GestureKind.Brightness -> {
                                val dy = -delta.y / verticalSensitivity
                                if (abs(dy) > 0.001f) {
                                    onBrightnessChange(dy)
                                }
                            }
                            GestureKind.Volume -> {
                                val dy = -delta.y / verticalSensitivity
                                if (abs(dy) > 0.001f) {
                                    onVolumeChange(dy)
                                }
                            }
                            GestureKind.None -> Unit
                        }
                    }
                )
            }
    ) {
        content()
    }
}

private enum class GestureKind { None, Seek, Brightness, Volume }
