package com.bhanuagrawal.kmplab.uploader

import kotlinx.coroutines.flow.Flow

internal interface UploadMetaRepo {

    suspend fun saveUploadRequest(uploadId: String, uploadRequest: UploadRequest)

    suspend fun saveChunks(uploadId: String, chunks: List<Chunk>)

    suspend fun getUploadMeta(uploadId: String): UploadMeta?

    suspend fun updateLocalPath(uploadId: String, newPath: String)

    suspend fun markChunkUploaded(uploadId: String, offset: Int, uploadedSize: Int)

    suspend fun markUploadCompleted(uploadId: String)

    fun getUploadState(requestId: String): Flow<UploadState>

    suspend fun markCancelled(requestId: String)

    suspend fun isCancelled(uploadId: String): Boolean

    suspend fun allChunksComplete(uploadId: String): Boolean

    suspend fun isCompleted(uploadId: String): Boolean

}
