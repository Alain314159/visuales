package com.alain.visuales.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A file or sub-directory entry inside a directory's listing.
 *
 * Scraped on demand from the server's Nginxy listing, with a TTL. Cached locally
 * so navigating back to a previously-seen folder is instant.
 */
@Entity(
    tableName = "directory_entries",
    indices = [
        Index("parent_path"),
        Index("type"),
        Index("size"),
        Index("ttl_expires_at")
    ],
    foreignKeys = [
        ForeignKey(
            entity = DirectoryEntity::class,
            parentColumns = ["path"],
            childColumns = ["parent_path"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DirectoryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Parent directory's path (FK to directories.path). */
    @ColumnInfo(name = "parent_path")
    val parentPath: String,

    /** URL-decoded display name, e.g. "Viva Cuba (2005) [1080p].mp4". */
    @ColumnInfo(name = "name")
    val name: String,

    /** Absolute URL of this entry on the server. */
    @ColumnInfo(name = "url")
    val url: String,

    /** URL-decoded path relative to the server, e.g. "/Peliculas/Cubanas/Viva Cuba (2005).mp4". */
    @ColumnInfo(name = "path")
    val path: String,

    /** True if this is a directory. */
    @ColumnInfo(name = "is_directory")
    val isDirectory: Boolean,

    /** Detected entry type (EntryType.name). */
    @ColumnInfo(name = "type")
    val type: String,

    /** Size in bytes (0 for directories or unknown). */
    @ColumnInfo(name = "size")
    val size: Long = 0,

    /** Last modified timestamp (epoch millis), or null if unknown. */
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long? = null,

    /** File extension (lowercase, no dot). */
    @ColumnInfo(name = "extension")
    val extension: String,

    /** When this entry was scraped (epoch millis). */
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long,

    /** When this entry's cache expires (epoch millis). */
    @ColumnInfo(name = "ttl_expires_at")
    val ttlExpiresAt: Long
)
