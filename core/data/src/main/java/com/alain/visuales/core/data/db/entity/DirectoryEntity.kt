package com.alain.visuales.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A directory on the UCLV Visuales server.
 *
 * Synchronized from `listado.html`. This is the "index" — it gives us the
 * navigable tree of the catalog. The contents (files) of each directory are
 * scraped on demand and stored in [DirectoryEntryEntity] with a TTL.
 */
@Entity(
    tableName = "directories",
    indices = [
        Index("parent_path"),
        Index("section"),
        Index("depth"),
        Index("added_at")
    ]
)
data class DirectoryEntity(
    /** URL-decoded path relative to the server, ending with "/". E.g. "/Peliculas/Cubanas/1938_Romance del Palmar/". */
    @PrimaryKey
    @ColumnInfo(name = "path")
    val path: String,

    /** Absolute URL on the server, e.g. "https://visuales.uclv.cu/Peliculas/Cubanas/1938_Romance%20del%20Palmar/". */
    @ColumnInfo(name = "url")
    val url: String,

    /** URL-decoded display name, e.g. "1938_Romance del Palmar". */
    @ColumnInfo(name = "name")
    val name: String,

    /** URL-decoded parent path, e.g. "/Peliculas/Cubanas/". Empty for the root. */
    @ColumnInfo(name = "parent_path")
    val parentPath: String,

    /** Depth in the tree (0 = root section). */
    @ColumnInfo(name = "depth")
    val depth: Int,

    /** First path segment, e.g. "Peliculas". Used for Home screen carousels. */
    @ColumnInfo(name = "section")
    val section: String,

    /** Second path segment if present, e.g. "Cubanas". */
    @ColumnInfo(name = "subcategory")
    val subcategory: String?,

    /** Number of sub-directories (precomputed from listado.html). */
    @ColumnInfo(name = "child_count")
    val childCount: Int = 0,

    /** When this directory was first detected by the sync worker (epoch millis). */
    @ColumnInfo(name = "added_at")
    val addedAt: Long,

    /** When this directory was last seen in a sync (epoch millis). */
    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long,

    /** Set to true if this directory was not present in the latest sync (for deletion). */
    @ColumnInfo(name = "is_stale")
    val isStale: Boolean = false
)
