package com.alain.visuales.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alain.visuales.core.data.db.entity.EmbyItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbyItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EmbyItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EmbyItemEntity>)

    @Query("SELECT * FROM emby_items WHERE path = :path LIMIT 1")
    fun findByPath(path: String): Flow<EmbyItemEntity?>

    @Query("SELECT * FROM emby_items WHERE item_id = :itemId LIMIT 1")
    suspend fun getById(itemId: String): EmbyItemEntity?

    @Query("SELECT * FROM emby_items WHERE ttl_expires_at < :now")
    suspend fun getExpired(now: Long): List<EmbyItemEntity>

    @Query("DELETE FROM emby_items WHERE ttl_expires_at < :now")
    suspend fun deleteExpired(now: Long): Int

    @Query("DELETE FROM emby_items")
    suspend fun clearAll()
}
