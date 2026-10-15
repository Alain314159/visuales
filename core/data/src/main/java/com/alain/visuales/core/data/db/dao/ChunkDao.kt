package com.alain.visuales.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alain.visuales.core.data.db.entity.ChunkEntity

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chunks: List<ChunkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chunk: ChunkEntity)

    @Query("SELECT * FROM download_chunks WHERE download_id = :downloadId ORDER BY `index` ASC")
    suspend fun chunksFor(downloadId: String): List<ChunkEntity>

    @Query("DELETE FROM download_chunks WHERE download_id = :downloadId")
    suspend fun deleteChunks(downloadId: String)

    @Query("DELETE FROM download_chunks WHERE download_id = :downloadId")
    suspend fun deleteForDownload(downloadId: String)
}
