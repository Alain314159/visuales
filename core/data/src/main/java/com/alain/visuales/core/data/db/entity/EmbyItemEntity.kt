package com.alain.visuales.core.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached Emby metadata for a single catalog item (movie / series / episode).
 *
 * Looked up by [path] which is the visuales.uclv.cu file path — Emby's own
 * `Path` field happens to match because both servers expose the same backend
 * filesystem. This lets us enrich the catalog UI (poster, overview, genres,
 * rating, runtime) without leaving the UCLV network.
 *
 * Rows have a 7-day TTL ([ttlExpiresAt]); the cleanup worker can prune them
 * via [com.alain.visuales.core.data.db.dao.EmbyItemDao.deleteExpired].
 */
@Entity(
    tableName = "emby_items",
    indices = [Index("path"), Index("ttl_expires_at")]
)
data class EmbyItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "overview")
    val overview: String?,

    @ColumnInfo(name = "genres")
    val genres: String,

    @ColumnInfo(name = "production_year")
    val productionYear: Int?,

    @ColumnInfo(name = "poster_tag")
    val posterTag: String?,

    @ColumnInfo(name = "backdrop_tag")
    val backdropTag: String?,

    @ColumnInfo(name = "community_rating")
    val communityRating: Double?,

    @ColumnInfo(name = "official_rating")
    val officialRating: String?,

    @ColumnInfo(name = "runtime_ms")
    val runtimeMs: Long?,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,

    @ColumnInfo(name = "ttl_expires_at")
    val ttlExpiresAt: Long
)
