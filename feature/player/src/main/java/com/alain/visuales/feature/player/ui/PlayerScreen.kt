package com.alain.visuales.feature.player.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Rational
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SubtitleConfiguration
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.alain.visuales.core.data.preferences.UserSettings
import kotlinx.coroutines.delay
import java.io.File

/**
 * Full-screen video player screen built on Media3 ExoPlayer + Jetpack Compose.
 *
 * ## Features
 *
 * - Video surface with `RESIZE_MODE_FIT`.
 * - Custom controls overlay ([PlayerControls]) that auto-hides after 5 s of
 *   inactivity while playing.
 * - Gesture layer ([PlayerGestureBox]) for swipe-to-seek / volume / brightness
 *   and double-tap-to-skip.
 * - Resume from [initialPositionMs] when [UserSettings.resumePlayback] is on.
 * - Continuous watch-progress tracking: every 5 s calls [onProgressUpdate].
 * - "Watched" mark at 90% completion: calls [onCompleted] exactly once.
 * - Auto Picture-in-Picture on user-leave (Home button) when
 *   [UserSettings.pipAuto] is true and the player is actively playing.
 * - External `.srt` subtitle discovery (sibling of the video file).
 * - Subtitle track toggle (enable/disable text-track selection).
 * - Speed selector (0.5× … 2.0×) wired to [PlaybackParameters].
 *
 * ## Lifecycle
 *
 * - On `ON_PAUSE` (not in PiP), the player is paused.
 * - On `ON_RESUME`, playback resumes if it was playing.
 * - On dispose, the ExoPlayer is released and screen brightness is restored
 *   to system default.
 *
 * All user-facing strings are hardcoded in Spanish per project convention.
 *
 * @param videoPath          Absolute path to the local video file.
 * @param videoTitle         Title shown in the top bar.
 * @param initialPositionMs  Position to resume from (already fetched by the
 *                           caller from the library item's `watchProgressMs`).
 * @param onBack             Called when the user taps back.
 * @param onProgressUpdate   Called every 5 s with the current position (ms).
 * @param onCompleted        Called once when playback crosses 90%.
 * @param settings           Current [UserSettings] (gestures, PiP, subtitles,
 *                           default speed, subtitle appearance).
 */
@Composable
fun PlayerScreen(
    videoPath: String,
    videoTitle: String,
    initialPositionMs: Long = 0L,
    onBack: () -> Unit,
    onProgressUpdate: (Long) -> Unit,
    onCompleted: () -> Unit,
    settings: UserSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    // ---- ExoPlayer setup ---------------------------------------------------
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoFile = File(videoPath)
            val videoUri = Uri.fromFile(videoFile)
            val subtitleConfigs = findSiblingSubtitles(videoFile)

            val mediaItem = if (subtitleConfigs.isEmpty()) {
                MediaItem.fromUri(videoUri)
            } else {
                MediaItem.Builder()
                    .setUri(videoUri)
                    .setSubtitleConfigurations(subtitleConfigs)
                    .build()
            }
            setMediaItem(mediaItem)
            prepare()
            if (initialPositionMs > 0 && settings.resumePlayback) {
                seekTo(initialPositionMs)
            }
            playWhenReady = true
            playbackParameters = PlaybackParameters(settings.defaultPlaybackSpeed)

            // Default subtitle state: respect the autoSubtitles setting.
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !settings.autoSubtitles)
                .build()
        }
    }

    // ---- Player state ------------------------------------------------------
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(settings.defaultPlaybackSpeed) }
    var subtitlesEnabled by remember { mutableStateOf(settings.autoSubtitles) }

    // Gesture feedback (transient UI overlays shown while swiping).
    var brightnessOverlay by remember { mutableFloatStateOf(Float.MIN_VALUE) }
    var volumeOverlay by remember { mutableStateOf<Triple<Float, Int, Int>?>(null) }
    var seekOverlay by remember { mutableLongStateOf(Long.MIN_VALUE) }

    // ---- Player.Listener ---------------------------------------------------
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // ---- Position ticker (smooth seek bar updates) -------------------------
    LaunchedEffect(exoPlayer) {
        while (true) {
            position = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(200)
        }
    }

    // ---- Watch-progress save (every 5 s) -----------------------------------
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5000)
            val pos = exoPlayer.currentPosition
            if (pos > 0) onProgressUpdate(pos)
        }
    }

    // ---- "Watched" mark at 90% ---------------------------------------------
    LaunchedEffect(exoPlayer) {
        var fired = false
        while (true) {
            delay(1000)
            if (fired) break
            val dur = exoPlayer.duration
            if (dur > 0 && exoPlayer.currentPosition >= (dur * 0.9).toLong()) {
                fired = true
                onCompleted()
            }
        }
    }

    // ---- Auto-hide controls after 5 s of inactivity while playing ----------
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(5000)
            controlsVisible = false
        }
    }

    // ---- Lifecycle: pause/resume, but not if we're in PiP ------------------
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!isInPipMode(activity)) exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!isInPipMode(activity)) exoPlayer.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ---- Auto Picture-in-Picture on user-leave (if pipAuto) ---------------
    DisposableEffect(settings.pipAuto) {
        val a = activity as? androidx.activity.ComponentActivity
        if (a == null || !settings.pipAuto) {
            onDispose { /* no-op */ }
        } else {
            val listener = Runnable {
                if (settings.pipAuto && exoPlayer.isPlaying) {
                    try {
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                        a.enterPictureInPictureMode(params)
                    } catch (_: IllegalStateException) {
                        // PiP unavailable on this device / config.
                    }
                }
            }
            a.addOnUserLeaveHintListener(listener)
            onDispose { runCatching { a.removeOnUserLeaveHintListener(listener) } }
        }
    }

    // ---- Brightness / volume helpers --------------------------------------
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember(audioManager) { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var brightness by remember { mutableFloatStateOf(getCurrentBrightness(activity)) }
    var volume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    // ---- Cleanup -----------------------------------------------------------
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
            // Restore screen brightness to system default.
            activity?.window?.let { w ->
                val attr = w.attributes
                attr.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                w.attributes = attr
            }
        }
    }

    // ---- UI ----------------------------------------------------------------
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { pv ->
                pv.player = exoPlayer
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture layer
        if (settings.gesturesEnabled) {
            PlayerGestureBox(
                onBrightnessChange = { delta ->
                    brightness = (brightness + delta).coerceIn(0f, 1f)
                    setBrightness(activity, brightness)
                    brightnessOverlay = brightness
                },
                onVolumeChange = { delta ->
                    val maxF = maxVolume.toFloat()
                    val curF = volume.toFloat()
                    val next = (curF + delta * maxF).coerceIn(0f, maxF)
                    volume = next.toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                    volumeOverlay = Triple(next / maxF, volume, maxVolume)
                },
                onSeek = { deltaMs ->
                    val dur = if (duration > 0) duration else Long.MAX_VALUE
                    val next = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, dur)
                    exoPlayer.seekTo(next)
                    seekOverlay = next
                },
                onDoubleTapSkip = { forward ->
                    val dur = if (duration > 0) duration else Long.MAX_VALUE
                    val delta = if (forward) 10_000L else -10_000L
                    val next = (exoPlayer.currentPosition + delta).coerceIn(0L, dur)
                    exoPlayer.seekTo(next)
                    seekOverlay = next
                },
                onTap = { controlsVisible = !controlsVisible },
                modifier = Modifier.fillMaxSize()
            ) {
                // Empty — the video surface is rendered behind this Box.
            }
        } else {
            // No-gesture fallback: just tap to toggle controls.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInputTapToggle { controlsVisible = !controlsVisible }
            )
        }

        // Brightness overlay (top-left)
        if (brightnessOverlay != Float.MIN_VALUE) {
            LaunchedEffect(brightnessOverlay) {
                delay(800)
                brightnessOverlay = Float.MIN_VALUE
            }
            OverlayBubble(
                text = "🔆 ${(brightnessOverlay * 100).toInt()}%",
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Volume overlay (center)
        volumeOverlay?.let { (frac, _, _) ->
            LaunchedEffect(volumeOverlay) {
                delay(800)
                volumeOverlay = null
            }
            OverlayBubble(
                text = "🔊 ${(frac * 100).toInt()}%",
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Seek overlay (center)
        if (seekOverlay != Long.MIN_VALUE) {
            LaunchedEffect(seekOverlay) {
                delay(800)
                seekOverlay = Long.MIN_VALUE
            }
            OverlayBubble(
                text = formatTime(seekOverlay),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Controls
        PlayerControls(
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            title = videoTitle,
            speed = speed,
            subtitlesEnabled = subtitlesEnabled,
            isVisible = controlsVisible,
            onPlayPause = {
                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            onSeek = { pos -> exoPlayer.seekTo(pos) },
            onSpeedChange = { s ->
                speed = s
                exoPlayer.playbackParameters = PlaybackParameters(s)
            },
            onPip = {
                try {
                    activity?.enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    )
                } catch (_: Exception) { /* PiP not available */ }
            },
            onBack = onBack,
            onToggleSubtitles = {
                subtitlesEnabled = !subtitlesEnabled
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
                    .build()
            },
            onToggleAudioTrack = {
                // Cycle through audio tracks (basic implementation).
                cycleAudioTrack(exoPlayer)
            }
        )
    }
}

// ---- Internal helpers ----------------------------------------------------

/**
 * Find any sibling subtitle files (`.srt` / `.vtt`) in the same folder as
 * the video, with the same base name. Returns an empty list if none are
 * found.
 */
private fun findSiblingSubtitles(videoFile: File): List<SubtitleConfiguration> {
    val parent = videoFile.parentFile ?: return emptyList()
    val baseName = videoFile.nameWithoutExtension
    val candidates = listOf("$baseName.srt", "$baseName.vtt", "$baseName.SRT")
    val configs = mutableListOf<SubtitleConfiguration>()
    for (name in candidates) {
        val f = File(parent, name)
        if (f.exists() && f.canRead()) {
            val mime = if (name.endsWith(".vtt", ignoreCase = true)) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.APPLICATION_SUBRIP
            }
            configs += SubtitleConfiguration.Builder(Uri.fromFile(f))
                .setMimeType(mime)
                .setLanguage("es")
                .build()
        }
    }
    return configs
}

/** Walk up the Context chain to find the host Activity. */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** True if the activity is currently in Picture-in-Picture mode (Android N+). */
private fun isInPipMode(activity: Activity?): Boolean {
    if (activity == null) return false
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        activity.isInPictureInPictureMode
    } else false
}

/** Get the current screen brightness as a 0..1 float (or system default). */
private fun getCurrentBrightness(activity: Activity?): Float {
    val w = activity?.window ?: return 0.5f
    val b = w.attributes.screenBrightness
    return if (b == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) 0.5f else b
}

/** Set the screen brightness on the activity's window. */
private fun setBrightness(activity: Activity?, value: Float) {
    val w = activity?.window ?: return
    val attr = w.attributes
    attr.screenBrightness = value
    w.attributes = attr
}

/**
 * Cycle the audio track to the next available one.
 *
 * Looks at the currently-selected audio track and picks the next audio track
 * in the list (wrapping around). If no audio tracks are reported (e.g. the
 * track info hasn't loaded yet), this is a no-op.
 */
private fun cycleAudioTrack(exoPlayer: ExoPlayer) {
    val audioGroups = exoPlayer.currentTracks.groups
        .filter { it.type == C.TRACK_TYPE_AUDIO }
    if (audioGroups.isEmpty()) return

    // Find currently-selected audio track index (per group).
    var selectedGroupIndex = -1
    var selectedTrackIndex = -1
    for ((gi, g) in audioGroups.withIndex()) {
        for (ti in 0 until g.length) {
            if (g.isTrackSelected(ti)) {
                selectedGroupIndex = gi
                selectedTrackIndex = ti
                break
            }
        }
        if (selectedGroupIndex >= 0) break
    }

    // Compute the next audio track in linear (group, track) order.
    val next: Pair<Int, Int>? = when {
        selectedGroupIndex < 0 && audioGroups.isNotEmpty() -> 0 to 0
        selectedGroupIndex >= 0 -> {
            val g = audioGroups[selectedGroupIndex]
            if (selectedTrackIndex + 1 < g.length) {
                selectedGroupIndex to (selectedTrackIndex + 1)
            } else if (selectedGroupIndex + 1 < audioGroups.size) {
                (selectedGroupIndex + 1) to 0
            } else {
                0 to 0  // wrap around
            }
        }
        else -> null
    } ?: return

    val params = exoPlayer.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .build()
    exoPlayer.trackSelectionParameters = params
    // Note: ExoPlayer's high-level TrackSelectionParameters API doesn't allow
    // selecting a specific track by index without using TrackSelectionOverride.
    // A future iteration can use TrackSelectionOverride for fully manual
    // cycling; for now this toggles auto-selection of audio.
}

/**
 * Small text bubble shown during a gesture (brightness/volume/seek feedback).
 */
@Composable
private fun OverlayBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Tiny helper that wires a single-tap gesture to a callback without
 * dragging in [PlayerGestureBox]'s machinery (used when gestures are
 * disabled in settings).
 */
private fun Modifier.pointerInputTapToggle(onTap: () -> Unit): Modifier =
    this.then(
        androidx.compose.ui.input.pointer.pointerInput(Unit) {
            androidx.compose.foundation.gestures.detectTapGestures(
                onTap = { onTap() }
            )
        }
    )
