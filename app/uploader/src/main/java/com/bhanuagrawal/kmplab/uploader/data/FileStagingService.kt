package com.bhanuagrawal.kmplab.uploader.data

import android.content.Context
import com.bhanuagrawal.kmplab.uploader.UploadMetaRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.UUID

/**
 * Resolves whatever URI was saved with an upload into a stable file:// URI
 * the SDK can stream from. For file:// URIs this is a no-op. For content://
 * URIs (or anything else not natively readable as a File) we copy the bytes
 * into a managed staging directory and rewrite the upload's localPath.
 */
internal interface FileStagingService {
    suspend fun stageIfNeeded(uploadId: String)
}

internal class FileStagingServiceImpl(
    private val context: Context,
    private val stagingDir: File,
    private val metaRepo: UploadMetaRepo,
) : FileStagingService {

    init {
        stagingDir.mkdirs()
    }

    override suspend fun stageIfNeeded(uploadId: String) {
        val meta = metaRepo.getUploadMeta(uploadId) ?: return
        val current = URI(meta.filepath)
        if (current.scheme.equals("file", ignoreCase = true)) return

        val newUri = stage(uploadId, current)
        metaRepo.updateLocalPath(uploadId, newUri.toString())
    }

    private suspend fun stage(uploadId: String, source: URI): URI = withContext(Dispatchers.IO) {
        val dest = File(stagingDir, "${uploadId}_${UUID.randomUUID()}.bin")
        val androidUri = android.net.Uri.parse(source.toString())
        context.contentResolver.openInputStream(androidUri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open $source")
        dest.toURI()
    }
}
