package com.alain.visuales.feature.downloads.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alain.visuales.core.data.db.entity.DownloadEntity
import com.alain.visuales.core.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Downloads screen.
 *
 * Exposes three [StateFlow]s (active / completed / failed) backed by the
 * corresponding [DownloadRepository] observe* methods, plus a small [speeds]
 * flow that maps download IDs to instantaneous download speeds (bytes/s).
 *
 * The speed flow exists because [DownloadEntity] does not persist speed; the
 * ViewModel derives a short-term estimate from successive emissions of
 * [DownloadRepository.observeActive] (Δbytes / Δtime), the same technique used
 * by [com.alain.visuales.feature.downloads.service.DownloadService] for the
 * notification.
 *
 * All mutations are forwarded to the repository and run on `viewModelScope`
 * (so the UI doesn't have to launch coroutines itself).
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repo: DownloadRepository
) : ViewModel() {

    val active: StateFlow<List<DownloadEntity>> = repo.observeActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    val completed: StateFlow<List<DownloadEntity>> = repo.observeCompleted()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    val failed: StateFlow<List<DownloadEntity>> = repo.observeFailed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    /**
     * Map of `downloadId → bytes/s`. Updated on every emission of [active].
     * Looked up by the screen when rendering the [DownloadCard] speed/ETA line.
     */
    private val _speeds = MutableStateFlow<Map<String, Long>>(emptyMap())
    val speeds: StateFlow<Map<String, Long>> = _speeds.asStateFlow()

    /** True when the user has pulled-to-refresh and we're "re-fetching". */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Track speeds as the active list evolves.
        viewModelScope.launch {
            repo.observeActive().collect { list ->
                updateSpeeds(list)
            }
        }
    }

    // ---- Commands ----------------------------------------------------

    fun pause(id: String) = viewModelScope.launch { repo.pause(id) }

    fun resume(id: String) = viewModelScope.launch { repo.resume(id) }

    fun cancel(id: String) = viewModelScope.launch { repo.cancel(id) }

    fun remove(id: String, deleteFile: Boolean = false) = viewModelScope.launch {
        repo.remove(id, deleteFile)
    }

    fun clearFinished() = viewModelScope.launch { repo.clearFinished() }

    /** Retry a failed download — under the hood this is just `resume(id)`. */
    fun retry(id: String) = viewModelScope.launch { repo.resume(id) }

    /**
     * Open [dl]'s file in an external video player via [FileProvider] +
     * [Intent.ACTION_VIEW].
     *
     * If the file doesn't exist on disk (e.g. it was deleted out-of-band or
     * the SAF folder mapping isn't honoured), a Toast is shown instead.
     *
     * @param context any context; the function uses [Context.getApplicationContext].
     */
    fun playVideo(context: Context, dl: DownloadEntity) {
        val appContext = context.applicationContext
        val file = repo.getFile(dl)
        if (!file.exists()) {
            Toast.makeText(appContext, "Archivo no encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        val authority = "${appContext.packageName}.fileprovider"
        val uri: Uri = try {
            FileProvider.getUriForFile(appContext, authority, file)
        } catch (_: IllegalArgumentException) {
            // File lives outside the FileProvider's declared paths.
            Toast.makeText(appContext, "No se puede abrir el archivo", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(appContext, "No hay reproductor de video", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Pull-to-refresh --------------------------------------------

    /**
     * Trigger a pull-to-refresh visual cycle. There's nothing to fetch (the
     * flows are already live), so we just toggle the [isRefreshing] flag for
     * ~600 ms to give the user visual feedback.
     */
    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(REFRESH_DELAY_MS)
            _isRefreshing.value = false
        }
    }

    // ---- Speed tracking ----------------------------------------------

    private fun updateSpeeds(active: List<DownloadEntity>) {
        val now = System.currentTimeMillis()
        val prev = _speeds.value
        val prevSamples = _speedSamples
        val updated = prev.toMutableMap()

        for (dl in active) {
            val sample = prevSamples[dl.id]
            val speed = if (sample != null) {
                val deltaBytes = dl.bytesDownloaded - sample.bytes
                val deltaMs = (now - sample.timestamp).coerceAtLeast(1L)
                (deltaBytes * 1000L) / deltaMs
            } else 0L
            updated[dl.id] = speed.coerceAtLeast(0L)
            _speedSamples[dl.id] = SpeedSample(dl.bytesDownloaded, now)
        }

        // Drop samples for downloads that left the active set so a later
        // resume starts fresh.
        val activeIds = active.map { it.id }.toSet()
        val iter = _speedSamples.entries.iterator()
        while (iter.hasNext()) {
            if (iter.next().key !in activeIds) iter.remove()
        }

        _speeds.value = updated
    }

    private val _speedSamples = HashMap<String, SpeedSample>()
    private data class SpeedSample(val bytes: Long, val timestamp: Long)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val REFRESH_DELAY_MS = 600L
    }
}
