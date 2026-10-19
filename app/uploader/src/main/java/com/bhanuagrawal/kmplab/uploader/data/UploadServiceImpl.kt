package com.bhanuagrawal.kmplab.uploader.data

import com.bhanuagrawal.kmplab.uploader.Immediate
import com.bhanuagrawal.kmplab.uploader.RemoteDataSource
import com.bhanuagrawal.kmplab.uploader.UploadMeta
import com.bhanuagrawal.kmplab.uploader.UploadMetaRepo
import com.bhanuagrawal.kmplab.uploader.UploadService

/**
 * Strategy-pattern Context: picks the right concrete [UploadService] per call,
 * based on [UploadMeta.priority] and [UploadMeta.foregroundOnly], and delegates.
 *
 * Selection rules:
 *  - foregroundOnly = true       → in-process coroutine service (WorkManager
 *                                  jobs may run after the app dies, violating
 *                                  the foreground-only contract)
 *  - priority = Immediate        → in-process coroutine service (user is waiting)
 *  - otherwise (Deferrable + bg) → WorkManager (survives process death,
 *                                  respects system constraints)
 */
internal class UploadServiceImpl(
    val coroutineBaseUploadService: CoroutineBaseUploadService,
    val workManagerBasedUploadService: WorkManagerBasedUploadService,
    override val remoteDataSource: RemoteDataSource,
    override val metaRepo: UploadMetaRepo,
) : UploadService() {

    override suspend fun upload(uploadMeta: UploadMeta) {
        pickStrategy(uploadMeta).upload(uploadMeta)
    }

    override fun cancel(uploadId: String) {
        // Strategy used isn't tracked, so cancel both — each is a no-op if N/A.
        coroutineBaseUploadService.cancel(uploadId)
        workManagerBasedUploadService.cancel(uploadId)
    }

    private fun pickStrategy(meta: UploadMeta): UploadService = when {
        meta.foregroundOnly -> coroutineBaseUploadService
        meta.priority is Immediate -> coroutineBaseUploadService
        else -> workManagerBasedUploadService
    }
}
