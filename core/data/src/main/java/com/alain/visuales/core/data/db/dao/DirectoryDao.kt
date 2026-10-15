package com.alain.visuales.core.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(directories: List<DirectoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(directory: DirectoryEntity)

    @Query("SELECT * FROM directories WHERE path = :path")
    suspend fun getByPath(path: String): DirectoryEntity?

    @Query("SELECT * FROM directories WHERE parent_path = :parentPath ORDER BY name COLLATE NOCASE ASC")
    fun children(parentPath: String): Flow<List<DirectoryEntity>>

    @Query("SELECT * FROM directories WHERE parent_path = :parentPath ORDER BY name COLLATE NOCASE ASC")
    suspend fun childrenSnapshot(parentPath: String): List<DirectoryEntity>

    @Query("SELECT * FROM directories WHERE section = :section ORDER BY name COLLATE NOCASE ASC")
    fun bySection(section: String): Flow<List<DirectoryEntity>>

    @Query("SELECT * FROM directories WHERE path LIKE :prefix || '%' ORDER BY depth ASC, name COLLATE NOCASE ASC")
    fun searchByPathPrefix(prefix: String): Flow<List<DirectoryEntity>>

    @Query("SELECT * FROM directories WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC LIMIT 200")
    fun search(query: String): Flow<List<DirectoryEntity>>

    @Query("SELECT * FROM directories ORDER BY added_at DESC LIMIT :limit")
    fun recentlyAdded(limit: Int): Flow<List<DirectoryEntity>>

    @Query("SELECT * FROM directories WHERE added_at > :since ORDER BY added_at DESC")
    fun addedSince(since: Long): Flow<List<DirectoryEntity>>

    @Query("SELECT section, COUNT(*) as count FROM directories WHERE section != '' GROUP BY section")
    fun sectionCounts(): Flow<List<SectionCount>>

    @Query("SELECT COUNT(*) FROM directories")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM directories WHERE added_at > :since")
    suspend fun countAddedSince(since: Long): Int

    /**
     * Bulk fetch all directories (just path + addedAt).
     *
     * Used by ListadoSyncWorker to preserve the original addedAt timestamps
     * across syncs without issuing 30k individual getByPath() calls.
     */
    @Query("SELECT * FROM directories")
    suspend fun getAll(): List<DirectoryEntity>

    @Query("UPDATE directories SET is_stale = 1 WHERE path NOT IN (:alivePaths)")
    suspend fun markStale(alivePaths: List<String>)

    @Query("DELETE FROM directories WHERE is_stale = 1")
    suspend fun deleteStale()

    @Query("UPDATE directories SET last_seen_at = :now WHERE path IN (:paths)")
    suspend fun touchSeen(paths: List<String>, now: Long)

    @Query("DELETE FROM directories")
    suspend fun clearAll()

    @Transaction
    suspend fun syncReplaceAll(directories: List<DirectoryEntity>, now: Long) {
        val paths = directories.map { it.path }
        markStale(paths)
        touchSeen(paths, now)
        upsertAll(directories)
        deleteStale()
    }
}

data class SectionCount(val section: String, val count: Int)
