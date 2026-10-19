package com.bhanuagrawal.kmplab.uploader


abstract class UploadService {

    abstract val remoteDataSource: RemoteDataSource
    internal abstract val metaRepo: UploadMetaRepo

    abstract suspend fun upload(uploadMeta: UploadMeta)

    /** Stop in-flight work for this upload. Default no-op; strategies override. */
    open fun cancel(uploadId: String) {}
}
