package com.bhanuagrawal.kmplab.ui.upload

import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhanuagrawal.kmplab.MyApp
import com.bhanuagrawal.kmplab.uploader.Immediate
import com.bhanuagrawal.kmplab.uploader.UploadRequest
import com.bhanuagrawal.kmplab.uploader.UploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import kotlin.random.Random

class UploadViewModel : ViewModel() {

    private val _ui = MutableStateFlow(UploadUi())
    val ui: StateFlow<UploadUi> = _ui.asStateFlow()

    fun setUrl(url: String) { _ui.update { it.copy(uploadUrl = url) } }

    fun generateTestFile(sizeBytes: Int) {
        viewModelScope.launch {
            val ctx = MyApp.instance
            val file = withContext(Dispatchers.IO) {
                val out = File(ctx.filesDir, "test_${System.currentTimeMillis()}.bin")
                out.outputStream().use { stream ->
                    var written = 0
                    val buf = ByteArray(64 * 1024)
                    while (written < sizeBytes) {
                        val toWrite = minOf(buf.size, sizeBytes - written)
                        Random.nextBytes(buf, 0, toWrite)
                        stream.write(buf, 0, toWrite)
                        written += toWrite
                    }
                }
                out
            }
            _ui.update {
                it.copy(
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    message = "Generated ${file.length()} bytes",
                )
            }
        }
    }

    /**
     * Hand the content:// URI to the SDK as-is — it copies into managed storage
     * during enqueueUploadRequest. We just remember the URI to upload.
     */
    fun onFilePicked(uri: Uri) {
        val displayName = queryDisplayName(uri) ?: "picked_${System.currentTimeMillis()}.bin"
        val baseUrl = _ui.value.uploadUrl.substringBeforeLast('/', missingDelimiterValue = _ui.value.uploadUrl)
        _ui.value = _ui.value.copy(
            pickedUri = uri.toString(),
            fileName = displayName,
            filePath = null,            // no longer a file path
            fileSizeBytes = 0,
            uploadUrl = "$baseUrl/$displayName",
            message = "Picked $displayName",
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cr = MyApp.instance.contentResolver
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return null
    }

    fun upload() {
        val s = _ui.value
        // Either a generated test file (filePath) or a picked URI (pickedUri).
        val fileUri: URI = when {
            s.pickedUri != null -> URI(s.pickedUri)
            s.filePath != null -> File(s.filePath).toURI()
            else -> {
                _ui.update { it.copy(message = "Pick or generate a file first") }
                return
            }
        }
        if (s.uploadUrl.isBlank()) {
            _ui.update { it.copy(message = "Enter an upload URL") }
            return
        }

        viewModelScope.launch {
            val sdk = MyApp.instance.uploadSdk
            val request = UploadRequest.Builder()
                .uploadUrl(s.uploadUrl)
                .fileURI(fileUri)
                .authHeader("Bearer test")
                .isForeground(true)
                .isForegroundOnly(true)
                .uploadPriority(Immediate())
                .build()
            val info = sdk.enqueueUploadRequest(request)
            _ui.update { it.copy(uploadId = info.uploadId, message = "Enqueued ${info.uploadId.take(8)}…") }

            sdk.observe(info.uploadId).collectLatest { state ->
                _ui.update { it.copy(state = state) }
            }
        }
    }

    fun cancel() {
        val id = _ui.value.uploadId ?: return
        viewModelScope.launch { MyApp.instance.uploadSdk.cancel(id) }
    }

    private fun MutableStateFlow<UploadUi>.update(transform: (UploadUi) -> UploadUi) {
        value = transform(value)
    }
}

data class UploadUi(
    val uploadUrl: String = "http://10.0.2.2:8080/test.bin",
    val filePath: String? = null,        // for test-generated files
    val pickedUri: String? = null,        // content:// URI from picker
    val fileName: String? = null,
    val fileSizeBytes: Long = 0,
    val uploadId: String? = null,
    val state: UploadState? = null,
    val message: String? = null,
)
