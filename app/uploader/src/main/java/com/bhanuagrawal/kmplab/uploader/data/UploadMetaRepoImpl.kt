package com.bhanuagrawal.kmplab.uploader.data

import com.bhanuagrawal.kmplab.uploader.Chunk
import com.bhanuagrawal.kmplab.uploader.Deferrable
import com.bhanuagrawal.kmplab.uploader.Immediate
import com.bhanuagrawal.kmplab.uploader.UploadMetaRepo
import com.bhanuagrawal.kmplab.uploader.UploadRequest
import com.bhanuagrawal.kmplab.uploader.UploadState
import com.bhanuagrawal.kmplab.uploader.UploadStatus
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadChunk
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadMeta
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadPriority as EntityUploadPriority
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadStatus as EntityUploadStatus
import com.bhanuagrawal.kmplab.uploader.ChunkState
import com.bhanuagrawal.kmplab.uploader.UploadMeta as DomainUploadMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.io.File
import java.net.URI
import java.util.UUID

internal class UploadMetaRepoImpl(
    private val uploadMetaDao: UploadMetaDao,
) : UploadMetaRepo {

    override suspend fun saveUploadRequest(uploadId: String, uploadRequest: UploadRequest) {
        uploadMetaDao.insertUploadMeta(uploadRequest.toUploadMeta(uploadId))
    }

    override suspend fun markChunkUploaded(uploadId: String, offset: Int, uploadedSize: Int) {
        uploadMetaDao.updateChunkProgress(uploadId, offset, uploadedSize)
    }

    override suspend fun markUploadCompleted(uploadId: String) {
        uploadMetaDao.markUploadCompleted(uploadId, System.currentTimeMillis())
    }

    override suspend fun updateLocalPath(uploadId: String, newPath: String) {
        uploadMetaDao.updateLocalPath(uploadId, newPath)
    }

    override suspend fun getUploadMeta(uploadId: String): DomainUploadMeta? {
        val entity = uploadMetaDao.getUploadMetaById(uploadId) ?: return null
        val chunkEntities = uploadMetaDao.getChunksForUpload(uploadId)
        return DomainUploadMeta(
            uploadId = entity.uploadId,
            filepath = entity.localPath,
            uploadUrl = entity.endPoint,
            header = entity.headers,
            chunks = chunkEntities.map { Chunk(totalSize = it.size, offset = it.offset) },
            priority = entity.priority.toPublicPriority(),
            foregroundOnly = entity.foregroundOnly,
        )
    }

    private fun EntityUploadPriority.toPublicPriority(): com.bhanuagrawal.kmplab.uploader.UploadPriority =
        when (this) {
            EntityUploadPriority.IMMEDIATE -> Immediate()
            EntityUploadPriority.DEFERRABLE -> Deferrable()
        }

    override suspend fun saveChunks(uploadId: String, chunks: List<Chunk>) {
        val entities = chunks.map { chunk ->
            UploadChunk(
                id = UUID.randomUUID().toString(),
                chunkUploadEndPoint = null,
                status = EntityUploadStatus.PENDING,
                uploadId = uploadId,
                offset = chunk.offset,
                size = chunk.totalSize,
                uploadedSize = 0,
            )
        }
        uploadMetaDao.insertChunks(entities)
    }

    override fun getUploadState(requestId: String): Flow<UploadState> {
        return combine(
            uploadMetaDao.getUploadStatus(requestId).filterNotNull(),
            uploadMetaDao.observeChunksForUpload(requestId),
        ) { status, chunks ->
            UploadState(
                totalChunks = chunks.size,
                completedChunks = chunks.count { it.uploadedSize >= it.size && it.size > 0 },
                totalSize = status.totalSize,
                uploadedSize = if (chunks.isEmpty()) status.uploadedBytes else chunks.sumOf { it.uploadedSize },
                chunksState = chunks.associate { c ->
                    c.id to ChunkState(
                        chunkOffset = c.offset,
                        size = c.size,
                        uploadedSize = c.uploadedSize,
                        status = c.status.toPublicStatus(isCancelled = status.isCancelled),
                    )
                },
                uploadStatus = status.status.toPublicStatus(status.isCancelled),
            )
        }
    }

    override suspend fun markCancelled(requestId: String) {
        uploadMetaDao.cancel(requestId)
    }

    override suspend fun isCancelled(uploadId: String): Boolean =
        uploadMetaDao.isCancelled(uploadId) == true

    override suspend fun allChunksComplete(uploadId: String): Boolean =
        uploadMetaDao.incompleteChunkCount(uploadId) == 0

    override suspend fun isCompleted(uploadId: String): Boolean =
        uploadMetaDao.completedCount(uploadId) > 0
}

// uploadRequest to uploadMeta mapper — populates with id
private fun UploadRequest.toUploadMeta(id: String): UploadMeta {
    val resolved = runCatching { File(URI(fileURI.toString())).length().toInt() }.getOrDefault(0)
    return UploadMeta(
        uploadId = id,
        endPoint = uploadUrl,
        headers = authHeader,
        localPath = fileURI.toString(),
        status = EntityUploadStatus.PENDING,
        uploadCompletedTime = 0L,
        retryCount = 0,
        totalSize = resolved,
        hasChunks = false,
        chunkSize = 0,
        chunkCount = 0,
        priority = uploadPriority.toEntityPriority(),
        foregroundOnly = isForegroundOnly,
        isCancelled = false,
    )
}

private fun com.bhanuagrawal.kmplab.uploader.UploadPriority.toEntityPriority(): EntityUploadPriority = when (this) {
    is Immediate -> EntityUploadPriority.IMMEDIATE
    is Deferrable -> EntityUploadPriority.DEFERRABLE
}

private fun EntityUploadStatus.toPublicStatus(isCancelled: Boolean): UploadStatus {
    if (isCancelled) return UploadStatus.CANCELLED
    return when (this) {
        EntityUploadStatus.PENDING -> UploadStatus.PENDING
        EntityUploadStatus.IN_PROGRESS -> UploadStatus.INPROGRESS
        EntityUploadStatus.PAUSED -> UploadStatus.PAUSED
        EntityUploadStatus.COMPLETED -> UploadStatus.SUCCESS
        EntityUploadStatus.FAILED -> UploadStatus.FAILED
        EntityUploadStatus.CANCELLED -> UploadStatus.CANCELLED
    }
}
