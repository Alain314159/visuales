package com.alain.visuales.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alain.visuales.core.data.db.entity.SyncStateEntity

@Dao
interface SyncStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)

    @Query("SELECT * FROM sync_state WHERE id = 0")
    suspend fun get(): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE id = 0")
    fun observe(): kotlinx.coroutines.flow.Flow<SyncStateEntity?>
    @Query("UPDATE sync_state SET last_sync_at = :timestamp WHERE id = 0")
    suspend fun setLastSyncAt(timestamp: Long)
}
