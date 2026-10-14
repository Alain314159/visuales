package com.alain.visuales.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local library entry — a file on the user's device.
 *
 * Populated by the library scanner (MediaStore + chosen folder scan).
 * Decoupled from [DownloadEntity] because the user might have copied files
 * manually to the folder.
 */
@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "size")
    val size: Long,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long? = null,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,

    @ColumnInfo(name = "watch_progress_ms")
    val watchProgressMs: Long = 0,

    @ColumnInfo(name = "is_watched")
    val isWatched: Boolean = false,

    @ColumnInfo(name = "added_at")
    val addedAt: Long,

    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long? = null
)
