package com.alain.visuales.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alain.visuales.core.data.db.entity.LibraryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LibraryItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LibraryItemEntity)

    @Update
    suspend fun update(item: LibraryItemEntity)

    @Query("SELECT * FROM library_items ORDER BY added_at DESC")
    fun observeAll(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE path = :path")
    suspend fun getByPath(path: String): LibraryItemEntity?

    @Query("SELECT * FROM library_items WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun search(query: String): Flow<List<LibraryItemEntity>>

    @Query("UPDATE library_items SET watch_progress_ms = :progress, last_played_at = :now WHERE path = :path")
    suspend fun updateWatchProgress(path: String, progress: Long, now: Long)

    @Query("UPDATE library_items SET is_watched = :watched WHERE path = :path")
    suspend fun setWatched(path: String, watched: Boolean)

    @Query("DELETE FROM library_items WHERE path = :path")
    suspend fun delete(path: String)

    @Query("DELETE FROM library_items")
    suspend fun clearAll()
}
