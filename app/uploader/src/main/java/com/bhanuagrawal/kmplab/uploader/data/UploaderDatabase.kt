package com.bhanuagrawal.kmplab.uploader.data

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadChunk
import com.bhanuagrawal.kmplab.uploader.data.entities.UploadMeta

@Database(
    entities = [UploadMeta::class, UploadChunk::class],
    version = 1,
    exportSchema = false,
)
internal abstract class UploaderDatabase : RoomDatabase() {
    abstract fun uploadMetaDao(): UploadMetaDao
}
