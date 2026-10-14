package com.alain.visuales.feature.player

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.media3.session.MediaSessionService

/**
 * Media3 MediaSessionService for background audio / PiP playback.
 *
 * Will be wired up when ExoPlayer integration is implemented in v1.5.
 */
class PlaybackService : MediaSessionService() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop playback when user dismisses the app from recents.
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
