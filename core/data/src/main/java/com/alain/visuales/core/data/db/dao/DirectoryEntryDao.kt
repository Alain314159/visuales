package com.alain.visuales.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alain.visuales.core.data.db.entity.DirectoryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectoryEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<DirectoryEntryEntity>)

    @Query("SELECT * FROM directory_entries WHERE parent_path = :parentPath ORDER BY is_directory DESC, name COLLATE NOCASE ASC")
    fun entriesFor(parentPath: String): Flow<List<DirectoryEntryEntity>>

    @Query("SELECT * FROM directory_entries WHERE parent_path = :parentPath")
    suspend fun snapshotFor(parentPath: String): List<DirectoryEntryEntity>

    @Query("SELECT COUNT(*) FROM directory_entries WHERE parent_path = :parentPath AND ttl_expires_at > :now")
    suspend fun countFresh(parentPath: String, now: Long): Int

    @Query("DELETE FROM directory_entries WHERE parent_path = :parentPath")
    suspend fun deleteFor(parentPath: String)

    @Query("DELETE FROM directory_entries WHERE ttl_expires_at < :now")
    suspend fun deleteExpired(now: Long): Int
}
