package com.bhanuagrawal.kmplab.uploader.data

import com.bhanuagrawal.kmplab.uploader.RemoteDataSource
import com.bhanuagrawal.kmplab.uploader.UploadMeta
import com.bhanuagrawal.kmplab.uploader.UploadMetaRepo
import com.bhanuagrawal.kmplab.uploader.UploadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.URI

internal class CoroutineBaseUploadService(
    override val remoteDataSource: RemoteDataSource,
    override val metaRepo: UploadMetaRepo,
) : UploadService() {

    override suspend fun upload(uploadMeta: UploadMeta) {
        val uri = URI(uploadMeta.filepath)

        if (uploadMeta.chunks.isEmpty()) {
            remoteDataSource.uploadFile(
                url = uploadMeta.uploadUrl,
                header = uploadMeta.header,
                fileUri = uri,
            )
        } else {
            supervisorScope {
                uploadMeta.chunks.forEach { chunk ->
                    launch {
                        try {
                            remoteDataSource.uploadChunk(
                                url = uploadMeta.uploadUrl,
                                header = uploadMeta.header,
                                fileUri = uri,
                                offset = chunk.offset,
                                size = chunk.totalSize,
                            )
                            metaRepo.markChunkUploaded(
                                uploadId = uploadMeta.uploadId,
                                offset = chunk.offset,
                                uploadedSize = chunk.totalSize,
                            )
                        } catch (c: CancellationException) {
                            throw c
                        } catch (e: Exception) {
                            // chunk failed; other chunks continue via supervisorScope
                        }
                    }
                }
            }
        }

        metaRepo.markUploadCompleted(uploadMeta.uploadId)
    }
}
