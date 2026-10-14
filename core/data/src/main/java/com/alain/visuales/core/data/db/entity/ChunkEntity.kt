package com.alain.visuales.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * State of a single byte-range chunk of a download.
 *
 * Multi-threaded downloads split the file into N chunks (typically 8). Each chunk
 * has its own OkHttp connection downloading a byte range. Persisting chunk state
 * allows resume after the app is killed or the phone reboots.
 */
@Entity(
    tableName = "download_chunks",
    indices = [Index("download_id")],
    foreignKeys = [
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["download_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "download_id")
    val downloadId: String,

    /** Chunk index 0..N-1. */
    @ColumnInfo(name = "index")
    val index: Int,

    /** Inclusive start byte in the file. */
    @ColumnInfo(name = "start_byte")
    val startByte: Long,

    /** Inclusive end byte in the file. */
    @ColumnInfo(name = "end_byte")
    val endByte: Long,

    /** Current position (last written byte + 1). */
    @ColumnInfo(name = "current_byte")
    val currentByte: Long,

    /** True if this chunk has been fully downloaded. */
    @ColumnInfo(name = "completed")
    val completed: Boolean = false
)
