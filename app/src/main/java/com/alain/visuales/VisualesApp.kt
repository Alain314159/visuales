package com.alain.visuales

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class. Sets up Hilt, WorkManager and notification channels.
 */
@HiltAndroidApp
class VisualesApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Active downloads (low importance, ongoing notifications)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DOWNLOADS_ACTIVE,
                getString(R.string.notif_channel_downloads_active),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de descargas en curso"
                setShowBadge(false)
            }
        )

        // Completed downloads (default importance, sound)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DOWNLOADS_COMPLETED,
                getString(R.string.notif_channel_downloads_completed),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones cuando una descarga termina"
                enableVibration(true)
            }
        )

        // Failed downloads (high importance to grab attention)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DOWNLOADS_FAILED,
                getString(R.string.notif_channel_downloads_failed),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando una descarga falla"
                enableVibration(true)
            }
        )

        // Catalog updates
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CATALOG_UPDATES,
                getString(R.string.notif_channel_catalog_updates),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Novedades en el catálogo de la UCLV"
            }
        )

        // App updates
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_APP_UPDATES,
                getString(R.string.notif_channel_app_updates),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nuevas versiones de la app disponibles"
            }
        )
    }

    companion object {
        const val CHANNEL_DOWNLOADS_ACTIVE = "downloads_active"
        const val CHANNEL_DOWNLOADS_COMPLETED = "downloads_completed"
        const val CHANNEL_DOWNLOADS_FAILED = "downloads_failed"
        const val CHANNEL_CATALOG_UPDATES = "catalog_updates"
        const val CHANNEL_APP_UPDATES = "app_updates"
    }
}
