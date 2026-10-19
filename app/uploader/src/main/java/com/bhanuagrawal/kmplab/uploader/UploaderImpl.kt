package com.bhanuagrawal.kmplab.uploader

import com.bhanuagrawal.kmplab.uploader.data.FileStagingService
import com.bhanuagrawal.kmplab.uploader.data.StagedFileCleanupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class UploaderImpl(
    val uploadMetaRepo: UploadMetaRepo,
    val remoteDataSource: RemoteDataSource,
    val chunkPolicy: ChunkPolicy,
    private val scope: CoroutineScope,
    private val uploadService: UploadService,
    private val fileStagingService: FileStagingService,
    private val cleanupService: StagedFileCleanupService,
) : Uploader {

    private val enqueueMutex = Mutex()

    // Per-upload pipeline jobs (coroutine strategy). Cancelling one stops staging,
    // probing and all in-flight chunks for that upload.
    private val jobs = ConcurrentHashMap<String, Job>()

    override suspend fun enqueueUploadRequest(uploadRequest: UploadRequest): UploadInfo =
        enqueueMutex.withLock {
            val uploadId = UUID.randomUUID().toString()

            // Persist immediately with whatever URI the caller gave (file://, content://, …).
            uploadMetaRepo.saveUploadRequest(uploadId, uploadRequest)

            val job = scope.launch {
                // Step 1 — resolve any non-file URI to a stable file URI in our managed dir.
                fileStagingService.stageIfNeeded(uploadId)

                // Step 2 — refetch meta (its localPath may have been rewritten by staging).
                val meta = uploadMetaRepo.getUploadMeta(uploadId) ?: return@launch
                val resolvedFileUri = URI(meta.filepath)

                // Step 3 — range probe + chunking based on the resolved file.
                val rangeSupported = remoteDataSource.isRangeSupported(
                    url = uploadRequest.uploadUrl,
                    header = uploadRequest.authHeader,
                )

                if (rangeSupported) {
                    val chunks = chunkPolicy.getChunk(resolvedFileUri)
                    uploadMetaRepo.saveChunks(uploadId, chunks)
                }

                // Step 4 — kick off the actual upload with the latest meta.
                uploadMetaRepo.getUploadMeta(uploadId)?.let { latest ->
                    uploadService.upload(latest)
                }

                // Step 5 — drop the staged copy, but ONLY if the upload actually
                // finished here. The coroutine path blocks until done (COMPLETED).
                // The WorkManager path returns immediately with workers still running,
                // so its staged file is reclaimed later by the boot-time orphan sweep.
                if (uploadMetaRepo.isCompleted(uploadId)) {
                    cleanupService.cleanupForUpload(uploadId)
                }
            }

            jobs[uploadId] = job
            job.invokeOnCompletion { jobs.remove(uploadId) }

            UploadInfo(uploadId = uploadId)
        }

    override fun observe(requestId: String): Flow<UploadState> {
        return uploadMetaRepo.getUploadState(requestId)
    }

    override suspend fun cancel(requestId: String) {
        // 1) durable flag — survives process death, drives observable status,
        //    and guards the WorkManager enqueue-vs-cancel race.
        uploadMetaRepo.markCancelled(requestId)
        // 2) coroutine strategy — cancel the pipeline job (downward to all chunks).
        jobs[requestId]?.cancel()
        // 3) workmanager strategy — cancel tagged workers. No-op if none.
        uploadService.cancel(requestId)
    }
}
