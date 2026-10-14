package com.alain.visuales.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alain.visuales.core.data.db.entity.ChunkEntity
import com.alain.visuales.core.data.db.entity.DownloadEntity
import com.alain.visuales.core.data.db.entity.DownloadPriority
import com.alain.visuales.core.data.db.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(download: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChunks(chunks: List<ChunkEntity>)

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','DOWNLOADING','PAUSED') ORDER BY priority DESC, created_at ASC")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completed_at DESC")
    fun observeCompleted(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'FAILED' ORDER BY created_at DESC")
    fun observeFailed(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','DOWNLOADING') ORDER BY priority DESC, created_at ASC LIMIT :limit")
    suspend fun getActive(limit: Int): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY priority DESC, created_at ASC LIMIT :limit")
    suspend fun getNextQueued(limit: Int): List<DownloadEntity>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'DOWNLOADING'")
    suspend fun countDownloading(): Int

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("UPDATE downloads SET progress = :progress, bytes_downloaded = :bytes WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, bytes: Long)

    @Query("UPDATE downloads SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatusAndError(id: String, status: String, error: String?)

    @Query("UPDATE downloads SET status = :status, completed_at = :completedAt WHERE id = :id")
    suspend fun setStatusAndCompletedAt(id: String, status: String, completedAt: Long?)

    @Query("UPDATE downloads SET retries = retries + 1 WHERE id = :id")
    suspend fun incrementRetries(id: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED','CANCELLED','FAILED')")
    suspend fun clearFinished()

    @Query("SELECT COUNT(*) FROM downloads")
    suspend fun count(): Int

    // ---- Chunks ----

    @Query("SELECT * FROM download_chunks WHERE download_id = :downloadId ORDER BY `index` ASC")
    suspend fun chunksFor(downloadId: String): List<ChunkEntity>

    @Query("UPDATE download_chunks SET current_byte = :currentByte, completed = :completed WHERE id = :chunkId")
    suspend fun updateChunkProgress(chunkId: Long, currentByte: Long, completed: Boolean)

    @Query("DELETE FROM download_chunks WHERE download_id = :downloadId")
    suspend fun deleteChunks(downloadId: String)

    @Query("SELECT COUNT(*) FROM download_chunks WHERE download_id = :downloadId AND completed = 1")
    suspend fun countCompletedChunks(downloadId: String): Int
}
