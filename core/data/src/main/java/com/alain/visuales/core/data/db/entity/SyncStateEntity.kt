package com.alain.visuales.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata of the last sync run (single row, id = 0).
 *
 * Used by the sync worker to skip re-downloading listado.html if the server's
 * ETag/Last-Modified hasn't changed.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey
    val id: Int = 0,

    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: Long? = null,

    @ColumnInfo(name = "listado_etag")
    val listadoEtag: String? = null,

    @ColumnInfo(name = "listado_last_modified")
    val listadoLastModified: Long? = null,

    @ColumnInfo(name = "listado_size")
    val listadoSize: Long = 0,

    @ColumnInfo(name = "total_directories")
    val totalDirectories: Int = 0,

    @ColumnInfo(name = "new_directories_last_sync")
    val newDirectoriesLastSync: Int = 0,

    @ColumnInfo(name = "removed_directories_last_sync")
    val removedDirectoriesLastSync: Int = 0,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null
)
