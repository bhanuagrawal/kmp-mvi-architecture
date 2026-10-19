package com.bhanuagrawal.kmplab.uploader

data class UploadMeta(
    val uploadId: String,
    val filepath: String,
    val uploadUrl: String,
    val header: String,
    val chunks: List<Chunk>,
    val priority: UploadPriority,
    val foregroundOnly: Boolean,
)
