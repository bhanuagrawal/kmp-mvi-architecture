package com.bhanuagrawal.kmplab.uploader.data.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey


//create coresponding enums for rooms
//make this room entity


@Entity(tableName = "upload_meta")
data class UploadMeta(
    @PrimaryKey
    val uploadId: String,
    val endPoint: String,
    val headers: String, //json
    val localPath: String, // URI
    val status: UploadStatus,
    val uploadCompletedTime: Long,
    val retryCount: Int,
    val totalSize: Int,
    val hasChunks: Boolean,
    val chunkSize: Int,
    val chunkCount: Int,
    val priority: UploadPriority,
    val foregroundOnly: Boolean,
    val isCancelled: Boolean,
)

enum class UploadStatus {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
}

enum class UploadPriority {
    IMMEDIATE,
    DEFERRABLE,
}
