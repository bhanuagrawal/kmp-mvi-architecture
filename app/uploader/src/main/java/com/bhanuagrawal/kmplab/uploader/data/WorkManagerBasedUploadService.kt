package com.bhanuagrawal.kmplab.uploader.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bhanuagrawal.kmplab.uploader.RemoteDataSource
import com.bhanuagrawal.kmplab.uploader.UploadMeta
import com.bhanuagrawal.kmplab.uploader.UploadMetaRepo
import com.bhanuagrawal.kmplab.uploader.UploadService

internal class WorkManagerBasedUploadService(
    private val context: Context,
    override val remoteDataSource: RemoteDataSource,
    override val metaRepo: UploadMetaRepo,
) : UploadService() {

    override suspend fun upload(uploadMeta: UploadMeta) {
        val workManager = WorkManager.getInstance(context)

        val chunkInputs = if (uploadMeta.chunks.isEmpty()) {
            // No range support: single worker uploads the whole file.
            listOf(WorkInput(offset = -1, size = -1))
        } else {
            uploadMeta.chunks.map { WorkInput(offset = it.offset, size = it.totalSize) }
        }

        chunkInputs.forEach { input ->
            val request = OneTimeWorkRequestBuilder<UploadChunkWorker>()
                .setInputData(
                    workDataOf(
                        KEY_UPLOAD_ID to uploadMeta.uploadId,
                        KEY_URL to uploadMeta.uploadUrl,
                        KEY_HEADER to uploadMeta.header,
                        KEY_FILE_PATH to uploadMeta.filepath,
                        KEY_OFFSET to input.offset,
                        KEY_SIZE to input.size,
                    )
                )
                .addTag(uploadMeta.uploadId)
                .build()
            workManager.enqueue(request)
        }
    }

    override fun cancel(uploadId: String) {
        // Cancels enqueued AND running workers carrying this tag.
        WorkManager.getInstance(context).cancelAllWorkByTag(uploadId)
    }

    private data class WorkInput(val offset: Int, val size: Int)

    companion object {
        const val KEY_UPLOAD_ID = "uploadId"
        const val KEY_URL = "url"
        const val KEY_HEADER = "header"
        const val KEY_FILE_PATH = "filepath"
        const val KEY_OFFSET = "offset"
        const val KEY_SIZE = "size"
    }
}

/**
 * Uploads a single chunk (or the whole file if offset/size is -1).
 *
 * Dependencies arrive via the constructor — wire them with [UploadWorkerFactory]
 * registered on WorkManager's Configuration at app startup.
 */
internal class UploadChunkWorker(
    appContext: Context,
    params: WorkerParameters,
    private val remoteDataSource: RemoteDataSource,
    private val metaRepo: UploadMetaRepo,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uploadId = inputData.getString(WorkManagerBasedUploadService.KEY_UPLOAD_ID)
            ?: return Result.failure()
        val url = inputData.getString(WorkManagerBasedUploadService.KEY_URL)
            ?: return Result.failure()
        val header = inputData.getString(WorkManagerBasedUploadService.KEY_HEADER)
            ?: return Result.failure()
        val filepath = inputData.getString(WorkManagerBasedUploadService.KEY_FILE_PATH)
            ?: return Result.failure()
        val offset = inputData.getInt(WorkManagerBasedUploadService.KEY_OFFSET, -1)
        val size = inputData.getInt(WorkManagerBasedUploadService.KEY_SIZE, -1)

        // Guard the enqueue-vs-cancel race: if the upload was cancelled before this
        // worker started (or across process death), bail before touching the network.
        if (metaRepo.isCancelled(uploadId)) return Result.failure()

        return try {
            val uri = java.net.URI(filepath)
            if (offset < 0 || size < 0) {
                remoteDataSource.uploadFile(url, header, uri)
                metaRepo.markUploadCompleted(uploadId)   // single-file: done in one worker
            } else {
                remoteDataSource.uploadChunk(url, header, uri, offset, size)
                metaRepo.markChunkUploaded(uploadId, offset, size)
                // The worker that lands the final chunk flips the upload to COMPLETED.
                // Idempotent — concurrent finishers just re-set the same row.
                if (metaRepo.allChunksComplete(uploadId)) {
                    metaRepo.markUploadCompleted(uploadId)
                }
            }
            Result.success()
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

/**
 * Register on WorkManager via Configuration.Builder().setWorkerFactory(...) so
 * UploadChunkWorker receives its dependencies through the constructor.
 */
internal class UploadWorkerFactory(
    private val remoteDataSource: RemoteDataSource,
    private val metaRepo: UploadMetaRepo,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        UploadChunkWorker::class.java.name -> UploadChunkWorker(
            appContext = appContext,
            params = workerParameters,
            remoteDataSource = remoteDataSource,
            metaRepo = metaRepo,
        )
        else -> null
    }
}
