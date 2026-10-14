package com.alain.visuales.feature.downloads.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.alain.visuales.core.data.db.entity.DownloadEntity
import com.alain.visuales.core.data.db.entity.DownloadStatus
import com.alain.visuales.core.data.repository.DownloadRepository
import com.alain.visuales.feature.downloads.notifications.DownloadNotifications
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Foreground service that owns the user-facing lifecycle of the download
 * subsystem.
 *
 * ## Responsibilities
 *
 * 1. **Keep the process alive** while at least one download is active
 *    (status `QUEUED`, `DOWNLOADING` or `PAUSED`). This is required for the
 *    multi-threaded download engine (in `:core:network`) to make progress
 *    while the user has backgrounded the app.
 *
 * 2. **Post a persistent summary notification** with the progress of the most
 *    recently-created active download. This notification *is* the foreground
 *    notification that keeps the service alive, and carries "Pausar todas"
 *    and "Cancelar" actions.
 *
 * 3. **Post per-download progress notifications** (via [DownloadNotifications])
 *    for the *other* active downloads (the most recent one is covered by the
 *    foreground summary).
 *
 * 4. **Fire terminal notifications** when a download transitions to
 *    [DownloadStatus.COMPLETED] or [DownloadStatus.FAILED].
 *
 * 5. **Stop itself** when the active set becomes empty.
 *
 * ## Action intents
 *
 * The service can be controlled via [Intent]s with the actions declared in the
 * [Companion] (`ACTION_PAUSE_ALL`, `ACTION_CANCEL_ALL`, `ACTION_PAUSE`,
 * `ACTION_CANCEL`, `ACTION_RESUME`). These are used by:
 *
 *  - The foreground summary notification ("Pausar todas" / "Cancelar").
 *  - The per-download progress notifications ("Pausar" / "Cancelar").
 *  - The failed-notification "Reintentar" action.
 *
 * ## Speed tracking
 *
 * The [DownloadEntity] table doesn't store the instantaneous download speed
 * (only `bytesDownloaded`). The service derives a short-term moving estimate
 * by snapshotting `(bytes, timestamp)` per download on every emission of
 * [DownloadRepository.observeAll] and computing `Δbytes / Δtime`. This is the
 * same approach used by [com.alain.visuales.feature.downloads.screens.DownloadsViewModel]
 * for the in-app cards.
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var repo: DownloadRepository
    @Inject lateinit var notifications: DownloadNotifications

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    /** Per-download (bytes, timestamp) snapshot for speed estimation. */
    private val speedSamples = HashMap<String, SpeedSample>()

    /** Previously-observed set of "active" download IDs, for transition detection. */
    private var previousActiveIds: Set<String> = emptySet()

    /** Previously-observed set of all download IDs, for removal detection. */
    private var previousAllIds: Set<String> = emptySet()

    /** True once the observer coroutine has been launched. */
    @Volatile private var observerStarted: Boolean = false
    private var observerJob: Job? = null

    // ---- Service lifecycle ---------------------------------------------------

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // The observer is launched lazily in onStartCommand so we don't spin
        // up a coroutine that immediately stops the service if the system
        // creates us just to deliver an action intent.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Become foreground immediately. The system requires this within
        //    ~5 s of starting a foreground service, otherwise it throws
        //    ForegroundServiceStartNotAllowedException.
        promoteToForeground(notifications.buildPlaceholderForeground())

        // 2. Start the observer (idempotent).
        startObserverIfNeeded()

        // 3. Handle the action (if any).
        intent?.action?.let { action ->
            handleAction(action, intent.getStringExtra(EXTRA_DOWNLOAD_ID))
        }

        // START_STICKY: if the system kills the service (low memory), restart
        // it so we can resume observing and re-establish the foreground notif.
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        observerJob = null
        observerStarted = false
        super.onDestroy()
    }

    // ---- Foreground management ----------------------------------------------

    private fun promoteToForeground(notification: Notification) {
        try {
            ServiceCompat.startForeground(
                this,
                DownloadNotifications.NOTIF_ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } catch (_: Exception) {
            // ForegroundServiceStartNotAllowedException (background launch
            // restriction on Android 12+) or SecurityException if the manifest
            // is misconfigured. We silently degrade to a non-foreground
            // service; downloads will still proceed but may be killed by the
            // system under memory pressure.
        }
    }

    // ---- Observer ------------------------------------------------------------

    private fun startObserverIfNeeded() {
        if (observerStarted) return
        observerStarted = true
        observerJob = scope.launch {
            repo.observeAll().collectLatest { all ->
                handleEmission(all)
            }
        }
    }

    private suspend fun handleEmission(all: List<DownloadEntity>) {
        mutex.withLock {
            val currentAllIds = all.map { it.id }.toSet()

            // 1. Cancel notifications for downloads that have been removed
            //    from the database entirely (e.g. user pressed "Eliminar").
            for (removedId in previousAllIds - currentAllIds) {
                notifications.cancel(removedId)
                speedSamples.remove(removedId)
            }

            val active: List<DownloadEntity> = all.filter { it.status in ACTIVE_STATUSES }
            val activeIds = active.map { it.id }.toSet()

            // 2. Transition detection: downloads that left the active set.
            for (dl in all) {
                if (dl.id !in previousActiveIds) continue
                if (dl.id in activeIds) continue
                // Was active, no longer active.
                when (dl.status) {
                    DownloadStatus.COMPLETED.name -> notifications.notifyCompleted(dl)
                    DownloadStatus.FAILED.name -> notifications.notifyFailed(
                        dl,
                        dl.error ?: "Error desconocido"
                    )
                    DownloadStatus.CANCELLED.name -> notifications.cancel(dl.id)
                    else -> { /* no-op */ }
                }
                // Drop the speed sample so a later resume starts fresh.
                speedSamples.remove(dl.id)
            }

            // 3. Manage the foreground notification.
            if (active.isEmpty()) {
                notifications.cancelForeground()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                previousActiveIds = emptySet()
                previousAllIds = emptySet()
                return@withLock
            }

            // Pick the most recent active download for the foreground summary.
            val mostRecent = active.maxByOrNull { it.createdAt } ?: active.first()
            val speed = computeSpeed(mostRecent)
            // Update the foreground notification (NOTIF_ID_FOREGROUND is fixed).
            // We can just post it; ServiceCompat.startForeground was already
            // used for the very first call in onStartCommand.
            notifications.refreshForeground(mostRecent, speed)

            // The most-recent download is already covered by the foreground
            // notification, so cancel its per-download notification to avoid
            // duplication.
            notifications.cancel(mostRecent.id)

            // 4. Post per-download progress notifications for the OTHER active
            //    downloads.
            for (dl in active) {
                if (dl.id == mostRecent.id) continue
                val s = computeSpeed(dl)
                notifications.notifyProgress(dl, s)
            }

            previousActiveIds = activeIds
            previousAllIds = currentAllIds
        }
    }

    // ---- Action handling -----------------------------------------------------

    private fun handleAction(action: String, downloadId: String?) {
        scope.launch {
            when (action) {
                ACTION_PAUSE_ALL -> {
                    repo.observeActive().first().forEach { repo.pause(it.id) }
                }
                ACTION_CANCEL_ALL -> {
                    repo.observeActive().first().forEach { repo.cancel(it.id) }
                }
                ACTION_PAUSE -> {
                    downloadId?.let { repo.pause(it) }
                }
                ACTION_CANCEL -> {
                    downloadId?.let { repo.cancel(it) }
                }
                ACTION_RESUME -> {
                    downloadId?.let { repo.resume(it) }
                }
            }
        }
    }

    // ---- Speed estimation ----------------------------------------------------

    private fun computeSpeed(dl: DownloadEntity): Long {
        val now = System.currentTimeMillis()
        val prev = speedSamples[dl.id]
        val speed = if (prev != null) {
            val deltaBytes = dl.bytesDownloaded - prev.bytes
            val deltaMs = (now - prev.timestamp).coerceAtLeast(1L)
            // bytes/ms -> bytes/s
            (deltaBytes * 1000L) / deltaMs
        } else 0L
        speedSamples[dl.id] = SpeedSample(dl.bytesDownloaded, now)
        return speed.coerceAtLeast(0L)
    }

    private data class SpeedSample(val bytes: Long, val timestamp: Long)

    companion object {
        // Action intents. Fully-qualified to avoid collisions with system actions.
        const val ACTION_PAUSE_ALL = "com.alain.visuales.downloads.action.PAUSE_ALL"
        const val ACTION_CANCEL_ALL = "com.alain.visuales.downloads.action.CANCEL_ALL"
        const val ACTION_PAUSE = "com.alain.visuales.downloads.action.PAUSE"
        const val ACTION_CANCEL = "com.alain.visuales.downloads.action.CANCEL"
        const val ACTION_RESUME = "com.alain.visuales.downloads.action.RESUME"

        const val EXTRA_DOWNLOAD_ID = "download_id"

        /** Downloads considered "active" (non-terminal). Stored as their
         *  [DownloadStatus.name] strings so they can be compared directly
         *  against [DownloadEntity.status] (which is a String column). */
        private val ACTIVE_STATUSES: Set<String> = setOf(
            DownloadStatus.QUEUED.name,
            DownloadStatus.DOWNLOADING.name,
            DownloadStatus.PAUSED.name
        )

        /**
         * Convenience launcher for callers outside the feature module
         * (e.g. `:feature:catalog` after enqueuing a download).
         *
         * Starts the service as a foreground service if it isn't already
         * running. Safe to call multiple times.
         */
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
