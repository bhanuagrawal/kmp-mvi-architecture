package com.bhanuagrawal.kmplab.uploader

import java.net.URI

interface RemoteDataSource {
    suspend fun isRangeSupported(url: String, header: String): Boolean

    suspend fun uploadChunk(
        url: String,
        header: String,
        fileUri: URI,
        offset: Int,
        size: Int,
    )

    suspend fun uploadFile(
        url: String,
        header: String,
        fileUri: URI,
    )
}
