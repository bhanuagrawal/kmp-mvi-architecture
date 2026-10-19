package com.bhanuagrawal.kmplab.uploader

data class UploadState (
    val totalChunks: Int,
    val completedChunks: Int,
    val totalSize: Int,
    val uploadedSize: Int,
    val chunksState: Map<String, ChunkState>,
    val uploadStatus: UploadStatus
)

data class ChunkState(
    val chunkOffset : Int,
    val size: Int,
    val uploadedSize: Int,
    val status: UploadStatus
)

enum class UploadStatus{
    PENDING, QUEUED, INPROGRESS, SUCCESS, PAUSED, FAILED_RESUMABLE, FAILED, CANCELLED
}