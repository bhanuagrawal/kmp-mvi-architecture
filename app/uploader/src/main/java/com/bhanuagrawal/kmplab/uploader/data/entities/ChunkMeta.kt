package com.bhanuagrawal.kmplab.uploader.data.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

//create coresponding enums for rooms
//make this room entity
@Entity(
    tableName = "upload_chunk",
    foreignKeys = [
        ForeignKey(
            entity = UploadMeta::class,
            parentColumns = ["uploadId"],
            childColumns = ["uploadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("uploadId")],
)
data class UploadChunk(
    @PrimaryKey
    val id: String,
    val chunkUploadEndPoint: String?, // determine or fixed
    val status: UploadStatus,
    val uploadId: String,  // Foreign Key
    val offset: Int,
    val size: Int,
    val uploadedSize: Int,
)
