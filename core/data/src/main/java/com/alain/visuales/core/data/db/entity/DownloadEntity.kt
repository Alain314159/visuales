package com.alain.visuales.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A download task persisted across app restarts.
 *
 * Each download is identified by a stable string ID (URL + started-at hash).
 * The download engine (multi-threaded OkHttp) writes its progress here and the
 * UI observes the table to update the UI.
 */
@Entity(
    tableName = "downloads",
    indices = [
        Index("status"),
        Index("priority"),
        Index("created_at")
    ]
)
data class DownloadEntity(
    /** Stable string ID (URL + timestamp hash). */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** Source URL on the UCLV server. */
    @ColumnInfo(name = "url")
    val url: String,

    /** Local file path (relative to the chosen downloads folder). */
    @ColumnInfo(name = "local_path")
    val localPath: String,

    /** Display name (also used as the local filename). */
    @ColumnInfo(name = "name")
    val name: String,

    /** Total size in bytes (from HEAD request). */
    @ColumnInfo(name = "size")
    val size: Long,

    /** Status: QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED. */
    @ColumnInfo(name = "status")
    val status: String,

    /** Priority: HIGH, NORMAL, LOW. Higher priority downloads first. */
    @ColumnInfo(name = "priority")
    val priority: String = DownloadPriority.NORMAL.name,

    /** Progress 0..100. */
    @ColumnInfo(name = "progress")
    val progress: Int = 0,

    /** Bytes downloaded so far. */
    @ColumnInfo(name = "bytes_downloaded")
    val bytesDownloaded: Long = 0,

    /** Number of parallel connections used by this download. */
    @ColumnInfo(name = "connections")
    val connections: Int = 8,

    /** Optional subtitle URL (auto-downloaded if non-null). */
    @ColumnInfo(name = "subtitle_url")
    val subtitleUrl: String? = null,

    /** When the download was enqueued (epoch millis). */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** When the download completed (epoch millis), or null. */
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    /** Error message if status is FAILED. */
    @ColumnInfo(name = "error")
    val error: String? = null,

    /** Number of retry attempts so far. */
    @ColumnInfo(name = "retries")
    val retries: Int = 0,

    /** Whether to only download on Wi-Fi. */
    @ColumnInfo(name = "wifi_only")
    val wifiOnly: Boolean = false,

    /** Whether to only download when charging. */
    @ColumnInfo(name = "charging_only")
    val chargingOnly: Boolean = false,

    /** Section (Peliculas / Series / ...) used for stats. */
    @ColumnInfo(name = "section")
    val section: String? = null,

    /** Cover art URL (poster) for display in the queue, if known. */
    @ColumnInfo(name = "poster_url")
    val posterUrl: String? = null
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

enum class DownloadPriority {
    LOW, NORMAL, HIGH
}
