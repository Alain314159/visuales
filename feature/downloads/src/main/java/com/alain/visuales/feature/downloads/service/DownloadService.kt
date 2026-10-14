package com.alain.visuales.feature.downloads.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service that runs the multi-threaded download engine.
 *
 * This is a placeholder — the actual download engine (OkHttp Range + RandomAccessFile
 * + WorkManager coordination) will be implemented in v0.5. For now we declare it
 * in the manifest so the manifest merger doesn't fail.
 */
class DownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
}
