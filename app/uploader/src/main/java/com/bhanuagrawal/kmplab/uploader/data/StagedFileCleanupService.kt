package com.bhanuagrawal.kmplab.uploader.data

import com.bhanuagrawal.kmplab.uploader.UploadMetaRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

/**
 * Deletes files we staged into our managed directory. Files outside that
 * directory (caller-supplied file:// URIs) are never touched.
 */
internal interface StagedFileCleanupService {

    /** Delete the staged file for this upload, if it lives in our staging dir. */
    suspend fun cleanupForUpload(uploadId: String)

    /** Delete staged files that aren't referenced by any current upload row. */
    suspend fun sweepOrphans()
}

internal class StagedFileCleanupServiceImpl(
    private val stagingDir: File,
    private val metaRepo: UploadMetaRepo,
    private val activePathsProvider: suspend () -> Set<String>,
) : StagedFileCleanupService {

    override suspend fun cleanupForUpload(uploadId: String) = withContext(Dispatchers.IO) {
        val meta = metaRepo.getUploadMeta(uploadId) ?: return@withContext
        val file = runCatching { File(URI(meta.filepath)) }.getOrNull() ?: return@withContext
        if (isInsideStagingDir(file)) file.delete()
    }

    override suspend fun sweepOrphans() = withContext(Dispatchers.IO) {
        val active = activePathsProvider()
        stagingDir.listFiles()?.forEach { f ->
            if (f.absolutePath !in active) f.delete()
        }
        Unit
    }

    private fun isInsideStagingDir(file: File): Boolean {
        val parent = file.canonicalFile.parentFile ?: return false
        return parent.canonicalPath == stagingDir.canonicalPath
    }
}
