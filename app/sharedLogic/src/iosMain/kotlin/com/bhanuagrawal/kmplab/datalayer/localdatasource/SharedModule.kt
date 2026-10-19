package com.bhanuagrawal.kmplab.datalayer.localdatasource

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.bhanuagrawal.kmplab.di.ShareModule
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun buildSharedModule(): ShareModule {
    return ShareModule(appDatabaseBuilder())
}

@OptIn(ExperimentalForeignApi::class)
fun appDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val fileManager = NSFileManager.defaultManager
    val documentsURL = fileManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val dbPath = "${documentsURL?.path}/app_database.db"

    return Room.databaseBuilder<AppDatabase>(name = dbPath)
}