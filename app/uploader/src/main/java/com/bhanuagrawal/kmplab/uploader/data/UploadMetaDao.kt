package com.bhanuagrawal.kmplab.uploader.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadChunk
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadMeta
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadStatus
import kotlinx.coroutines.flow.Flow

data class UploadStatusView(
    val uploadId: String,
    val status: UploadStatus,
    val totalSize: Int,
    val uploadedBytes: Int,
    val retryCount: Int,
    val isCancelled: Boolean,
    val uploadCompletedTime: Long,
)

@Dao
interface UploadMetaDao {

    @Insert
    suspend fun insertUploadMeta(uploadMeta: UploadMeta)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<UploadChunk>)

    @Query("SELECT * FROM upload_meta WHERE uploadId = :uploadId")
    suspend fun getUploadMetaById(uploadId: String): UploadMeta?

    @Query("SELECT * FROM upload_chunk WHERE uploadId = :uploadId")
    suspend fun getChunksForUpload(uploadId: String): List<UploadChunk>

    @Query("SELECT * FROM upload_chunk WHERE uploadId = :uploadId ORDER BY offset ASC")
    fun observeChunksForUpload(uploadId: String): Flow<List<UploadChunk>>

    @Query("""
        UPDATE upload_chunk
        SET uploadedSize = :uploadedSize,
            status = CASE WHEN :uploadedSize >= size THEN 'COMPLETED' ELSE 'IN_PROGRESS' END
        WHERE uploadId = :uploadId AND offset = :offset
    """)
    suspend fun updateChunkProgress(uploadId: String, offset: Int, uploadedSize: Int)

    @Query("""
        UPDATE upload_meta
        SET status = 'COMPLETED',
            uploadCompletedTime = :completedAt
        WHERE uploadId = :uploadId
    """)
    suspend fun markUploadCompleted(uploadId: String, completedAt: Long)

    @Query("UPDATE upload_meta SET localPath = :path WHERE uploadId = :uploadId")
    suspend fun updateLocalPath(uploadId: String, path: String)

    @Query("SELECT localPath FROM upload_meta WHERE status != 'COMPLETED' AND isCancelled = 0")
    suspend fun activeLocalPaths(): List<String>

    @Query(
        """
        SELECT
            u.uploadId AS uploadId,
            u.status AS status,
            u.totalSize AS totalSize,
            COALESCE(SUM(c.uploadedSize), 0) AS uploadedBytes,
            u.retryCount AS retryCount,
            u.isCancelled AS isCancelled,
            u.uploadCompletedTime AS uploadCompletedTime
        FROM upload_meta u
        LEFT JOIN upload_chunk c ON c.uploadId = u.uploadId
        WHERE u.uploadId = :uploadId
        GROUP BY u.uploadId
        """
    )
    fun getUploadStatus(uploadId: String): Flow<UploadStatusView?>

    @Query("UPDATE upload_meta SET isCancelled = 1 WHERE uploadId = :uploadId")
    suspend fun cancel(uploadId: String)

    @Query("SELECT isCancelled FROM upload_meta WHERE uploadId = :uploadId")
    suspend fun isCancelled(uploadId: String): Boolean?

    @Query("SELECT COUNT(*) FROM upload_chunk WHERE uploadId = :uploadId AND uploadedSize < size")
    suspend fun incompleteChunkCount(uploadId: String): Int

    @Query("SELECT COUNT(*) FROM upload_meta WHERE uploadId = :uploadId AND status = 'COMPLETED'")
    suspend fun completedCount(uploadId: String): Int
}
