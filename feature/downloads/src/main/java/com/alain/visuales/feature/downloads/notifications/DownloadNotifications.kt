package com.alain.visuales.feature.downloads.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.common.formatBytes
import com.alain.visuales.core.common.formatEta
import com.alain.visuales.core.common.formatSpeed
import com.alain.visuales.core.data.db.entity.DownloadEntity
import com.alain.visuales.core.data.db.entity.DownloadStatus
import com.alain.visuales.feature.downloads.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts notifications for the download subsystem.
 *
 * Three notification "kinds" exist:
 *  - **Progress**: one ongoing notification per active download, with a
 *    [NotificationCompat.Builder.setProgress] bar and Pausar / Cancelar actions.
 *  - **Completed**: a single dismissable notification with a "Reproducir" action
 *    that opens the file in an external video player via [FileProvider].
 *  - **Failed**: a single dismissable notification with a "Reintentar" action
 *    that bounces back to [DownloadService] with [DownloadService.ACTION_RESUME].
 *
 * The foreground service also re-uses [buildForegroundNotification] for the
 * persistent summary notification that keeps the service alive.
 *
 * ## Channel IDs
 *
 * The notification channels themselves are created in `:app` by
 * `com.alain.visuales.VisualesApp.onCreate` (see `VisualesApp.kt`). Because the
 * `:feature:downloads` module cannot depend on `:app`, the channel IDs are
 * duplicated here as private string literals and **MUST** match the constants in
 * `VisualesApp.Companion`:
 *
 * | This file                   | VisualesApp.Companion                |
 * |-----------------------------|--------------------------------------|
 * | `CHANNEL_ACTIVE`            | `CHANNEL_DOWNLOADS_ACTIVE`           |
 * | `CHANNEL_COMPLETED`          | `CHANNEL_DOWNLOADS_COMPLETED`         |
 * | `CHANNEL_FAILED`             | `CHANNEL_DOWNLOADS_FAILED`           |
 *
 * If you rename them in `:app`, rename them here too or notifications will
 * silently fail to post on Android 8+.
 */
@Singleton
class DownloadNotifications @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nm = NotificationManagerCompat.from(context)

    // ---- Channel IDs (must match VisualesApp.Companion) ----------------------

    private object Channels {
        const val ACTIVE = "downloads_active"
        const val COMPLETED = "downloads_completed"
        const val FAILED = "downloads_failed"
    }

    // ---- Public API ----------------------------------------------------------

    /**
     * Post (or update) the per-download progress notification.
     *
     * The notification is [NotificationCompat.Builder.setOngoing] so it can't
     * be swiped away while the download is active. Includes "Pausar" and
     * "Cancelar" actions that bounce back to [DownloadService].
     */
    fun notifyProgress(dl: DownloadEntity, speedBytesPerSec: Long) {
        if (!nm.areNotificationsEnabled()) return
        val notif = buildProgressBuilder(dl, speedBytesPerSec, summary = false).build()
        try {
            nm.notify(notifIdFor(dl.id), notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied at runtime; nothing we can do.
        }
    }

    /**
     * Build the notification used to keep the foreground service alive.
     *
     * Shows the same progress info as [notifyProgress] but with summary-level
     * actions ("Pausar todas" and "Cancelar todas") instead of per-download
     * actions. The service posts this under the fixed [NOTIF_ID_FOREGROUND] id.
     */
    fun buildForegroundNotification(dl: DownloadEntity, speedBytesPerSec: Long): Notification {
        return buildProgressBuilder(dl, speedBytesPerSec, summary = true).build()
    }

    /**
     * Update (or post) the persistent foreground summary notification with the
     * progress of [dl]. Used by [DownloadService] on every emission of the
     * active downloads flow.
     */
    fun refreshForeground(dl: DownloadEntity, speedBytesPerSec: Long) {
        if (!nm.areNotificationsEnabled()) return
        val notif = buildForegroundNotification(dl, speedBytesPerSec)
        try {
            nm.notify(NOTIF_ID_FOREGROUND, notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied at runtime; the foreground service
            // keeps running but its notification is invisible.
        }
    }

    /**
     * Build a minimal placeholder notification used for the very first
     * [android.app.Service.startForeground] call before the active set has been
     * observed. The system requires a foreground notification within 5 s of
     * starting a foreground service.
     */
    fun buildPlaceholderForeground(): Notification {
        return NotificationCompat.Builder(context, Channels.ACTIVE)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargas")
            .setContentText("Preparando…")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Cancel the persistent foreground notification (if any). */
    fun cancelForeground() {
        nm.cancel(NOTIF_ID_FOREGROUND)
    }

    /**
     * Post a "completed" notification for [dl] with a "Reproducir" action
     * that opens the file in an external video player via [FileProvider].
     *
     * Also cancels any previously-posted progress notification for [dl.id]
     * so we don't leave a stale "downloading" notification around.
     */
    fun notifyCompleted(dl: DownloadEntity) {
        nm.cancel(notifIdFor(dl.id))
        if (!nm.areNotificationsEnabled()) return

        val playIntent = buildPlayIntent(dl)
        val playPi = PendingIntent.getActivity(
            context,
            // Unique request code per download so we don't clobber an existing PI.
            dl.id.hashCode() xor REQ_PLAY_MASK,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, Channels.COMPLETED)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Descarga completada")
            .setContentText(dl.name)
            .setSubText(formatBytes(dl.size))
            .setContentIntent(playPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                android.R.drawable.ic_media_play,
                "Reproducir",
                playPi
            )
            .build()

        try {
            nm.notify(notifIdFor(dl.id), notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied.
        }
    }

    /**
     * Post a "failed" notification for [dl] with a "Reintentar" action that
     * sends [DownloadService.ACTION_RESUME] back to the service.
     */
    fun notifyFailed(dl: DownloadEntity, error: String) {
        nm.cancel(notifIdFor(dl.id))
        if (!nm.areNotificationsEnabled()) return

        val retryIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, dl.id)
        }
        val retryPi = PendingIntent.getService(
            context,
            dl.id.hashCode() xor REQ_RETRY_MASK,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, Channels.FAILED)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Descarga fallida")
            .setContentText(dl.name)
            .setSubText(error.take(120))
            .setContentIntent(retryPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.stat_sys_download,
                "Reintentar",
                retryPi
            )
            .build()

        try {
            nm.notify(notifIdFor(dl.id), notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied.
        }
    }

    /** Cancel the per-download notification (progress / completed / failed). */
    fun cancel(id: String) {
        nm.cancel(notifIdFor(id))
    }

    // ---- Internals -----------------------------------------------------------

    private fun buildProgressBuilder(
        dl: DownloadEntity,
        speedBytesPerSec: Long,
        summary: Boolean
    ): NotificationCompat.Builder {
        val progress = dl.progress.coerceIn(0, 100)
        val downloaded = dl.bytesDownloaded.coerceAtLeast(0L)
        val size = dl.size

        // Format the second line:
        //   "1.2 GB / 4.5 GB  ·  42%  ·  3.4 MB/s  ·  4m 12s"
        val parts = mutableListOf<String>()
        if (size > 0) {
            parts += "${formatBytes(downloaded)} / ${formatBytes(size)}"
        } else {
            parts += formatBytes(downloaded)
        }
        parts += "$progress%"
        if (speedBytesPerSec > 0) {
            parts += formatSpeed(speedBytesPerSec)
            if (size > 0 && speedBytesPerSec > 0) {
                val remainingBytes = (size - downloaded).coerceAtLeast(0L)
                val etaMs = (remainingBytes * 1000L) / speedBytesPerSec
                parts += formatEta(etaMs)
            }
        }
        val contentText = parts.joinToString("  ·  ")

        val builder = NotificationCompat.Builder(context, Channels.ACTIVE)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(dl.name)
            .setContentText(contentText)
            .setProgress(100, progress, progress == 0 && dl.status == DownloadStatus.DOWNLOADING.name)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (summary) {
            // Foreground summary: pause-all / cancel-all actions.
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pausar todas",
                servicePendingIntent(
                    DownloadService.ACTION_PAUSE_ALL,
                    requestCode = REQ_PAUSE_ALL,
                    downloadId = null
                )
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancelar",
                servicePendingIntent(
                    DownloadService.ACTION_CANCEL_ALL,
                    requestCode = REQ_CANCEL_ALL,
                    downloadId = null
                )
            )
        } else {
            // Per-download: pause / cancel for this specific download.
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pausar",
                servicePendingIntent(
                    DownloadService.ACTION_PAUSE,
                    requestCode = dl.id.hashCode() xor REQ_PAUSE_MASK,
                    downloadId = dl.id
                )
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancelar",
                servicePendingIntent(
                    DownloadService.ACTION_CANCEL,
                    requestCode = dl.id.hashCode() xor REQ_CANCEL_MASK,
                    downloadId = dl.id
                )
            )
        }

        return builder
    }

    /**
     * Build a PendingIntent that fires [DownloadService] with the given
     * [action] and (optionally) the download id.
     */
    private fun servicePendingIntent(
        action: String,
        requestCode: Int,
        downloadId: String?
    ): PendingIntent {
        val intent = Intent(context, DownloadService::class.java).apply {
            this.action = action
            if (downloadId != null) {
                putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
            }
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Build an [Intent.ACTION_VIEW] intent that opens [dl]'s file in an
     * external video player, granting read access via the [FileProvider].
     *
     * Authority is `"${context.packageName}.fileprovider"` (matches the
     * `<provider>` declared in `:app/AndroidManifest.xml`).
     */
    private fun buildPlayIntent(dl: DownloadEntity): Intent {
        val file = resolveLocalFile(dl)
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = try {
            FileProvider.getUriForFile(context, authority, file)
        } catch (_: IllegalArgumentException) {
            // File is outside the paths declared in file_paths.xml; fall back
            // to a file:// URI (only works on API <= 24 before strict mode).
            Uri.fromFile(file)
        }
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Resolve the on-disk [File] for [dl].
     *
     * Mirrors `DownloadRepository.getFile(dl)` — downloads are written to
     * `Environment.DIRECTORY_DOWNLOADS/Visuales/<sanitized name>`. If the user
     * has configured a SAF folder (SettingsRepository.downloadFolderUri), the
     * file won't be accessible via [FileProvider]; in that case the play
     * intent will fail at runtime and the user gets a Toast from the UI.
     */
    private fun resolveLocalFile(dl: DownloadEntity): File {
        val baseDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            AppConstants.DOWNLOAD_FOLDER_NAME
        )
        return File(baseDir, sanitizeFilename(dl.name))
    }

    private fun sanitizeFilename(name: String): String {
        val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return normalized.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    private fun notifIdFor(downloadId: String): Int = downloadId.hashCode()

    companion object {
        /** Fixed notification id used by [DownloadService] for the foreground
         *  summary notification. Per-download progress notifications use
         *  `downloadId.hashCode()` and never collide with this constant. */
        const val NOTIF_ID_FOREGROUND = 0x7D00

        // Pending-intent request-code masks so action intents don't collide.
        private const val REQ_PAUSE_ALL = 0x10_000
        private const val REQ_CANCEL_ALL = 0x20_000
        private const val REQ_PAUSE_MASK = 0x30_000
        private const val REQ_CANCEL_MASK = 0x40_000
        private const val REQ_PLAY_MASK = 0x50_000
        private const val REQ_RETRY_MASK = 0x60_000
    }
}
