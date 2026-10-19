package com.bhanuagrawal.kmplab.uploader

import kotlinx.coroutines.flow.Flow

interface Uploader{

    suspend fun enqueueUploadRequest(uploadRequest: UploadRequest): UploadInfo

    fun observe(requestId: String): Flow<UploadState>

    suspend fun cancel(requestId: String)

}