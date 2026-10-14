package com.alain.visuales.feature.player

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.alain.visuales.core.data.preferences.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Media3 [MediaSessionService] that owns the background ExoPlayer + [MediaSession].
 *
 * ## Responsibilities
 *
 * 1. **Keep audio alive in the background.** When the user leaves the player UI
 *    (e.g. navigates to another app or the OS reclaims the UI), audio continues
 *    playing as long as this service is running.
 *
 * 2. **Expose a [MediaSession]** so external controllers (e.g. Bluetooth media
 *    buttons, Android Auto, Assistant, notification media controls) can drive
 *    playback. The session is created lazily in [onCreate] and torn down in
 *    [onDestroy].
 *
 * 3. **Stop itself** when the user dismisses the app from recents and playback
 *    is idle — see [onTaskRemoved].
 *
 * ## How the UI talks to this service
 *
 * The player UI (in `:feature:player`) creates its own short-lived [ExoPlayer]
 * for video rendering. For audio-only background playback, a separate code
 * path may bind to this service via [MediaSession.ControllerInfo]. The current
 * v1 implementation keeps the player UI self-contained; this service exists
 * primarily to satisfy the manifest declaration (`foregroundServiceType=
 * mediaPlayback`) and to provide a hook for future background-audio work.
 *
 * ## Lifecycle
 *
 * - [onCreate] builds the [ExoPlayer] and the [MediaSession].
 * - [onGetSession] returns the session for any controller that asks.
 * - [onTaskRemoved] stops playback and the service when the user swipes the
 *   app away from recents.
 * - [onDestroy] releases the player + session.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var settings: SettingsRepository

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val p = ExoPlayer.Builder(this).build()
        player = p
        mediaSession = MediaSession.Builder(this, p).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // If playback has finished (or never started), stop the service when
        // the user dismisses the app from recents. Otherwise keep it alive so
        // background audio continues.
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}
