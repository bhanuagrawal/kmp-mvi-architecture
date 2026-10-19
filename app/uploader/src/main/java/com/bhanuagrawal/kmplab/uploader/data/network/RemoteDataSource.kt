package com.bhanuagrawal.kmplab.uploader.data.network

import com.bhanuagrawal.kmplab.uploader.RemoteDataSource
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Url
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URI

internal interface UploadApi {
    @HEAD
    suspend fun head(
        @Url url: String,
        @Header("Authorization") authHeader: String,
    ): Response<Unit>

    @PUT
    suspend fun put(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Header("Content-Range") contentRange: String?,
        @Body body: RequestBody,
    ): Response<Unit>
}

internal class RetrofitRemoteDataSource(
    private val api: UploadApi,
) : RemoteDataSource {

    override suspend fun isRangeSupported(url: String, header: String): Boolean {
        val response = api.head(url = url, authHeader = header)
        if (!response.isSuccessful) return false
        val acceptRanges = response.headers()["Accept-Ranges"] ?: return false
        return acceptRanges.equals("bytes", ignoreCase = true)
    }

    override suspend fun uploadChunk(
        url: String,
        header: String,
        fileUri: URI,
        offset: Int,
        size: Int,
    ) {
        val file = File(fileUri)
        val body = FileRangeRequestBody(file, offset.toLong(), size.toLong(), OCTET_STREAM)
        val contentRange = "bytes $offset-${offset + size - 1}/${file.length()}"
        val response = api.put(url = url, authHeader = header, contentRange = contentRange, body = body)
        if (!response.isSuccessful) {
            throw IOException("Chunk PUT failed: HTTP ${response.code()} offset=$offset size=$size")
        }
    }

    override suspend fun uploadFile(url: String, header: String, fileUri: URI) {
        val file = File(fileUri)
        val body = file.asRequestBody(OCTET_STREAM)
        val response = api.put(url = url, authHeader = header, contentRange = null, body = body)
        if (!response.isSuccessful) {
            throw IOException("File PUT failed: HTTP ${response.code()}")
        }
    }

    private companion object {
        val OCTET_STREAM = "application/octet-stream".toMediaTypeOrNull()
    }
}

/**
 * RequestBody that streams a byte range of a file directly into the network sink.
 * Avoids loading the chunk into memory — OkHttp pulls in segments via Okio.
 */
private class FileRangeRequestBody(
    private val file: File,
    private val offset: Long,
    private val byteCount: Long,
    private val contentType: MediaType?,
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = byteCount

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(STREAM_BUFFER)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            var remaining = byteCount
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val read = raf.read(buffer, 0, toRead)
                if (read == -1) break
                sink.write(buffer, 0, read)
                remaining -= read
            }
        }
    }

    private companion object {
        const val STREAM_BUFFER = 8 * 1024 // 8 KB streaming buffer
    }
}
