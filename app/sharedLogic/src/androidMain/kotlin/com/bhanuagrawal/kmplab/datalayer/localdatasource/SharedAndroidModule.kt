package com.bhanuagrawal.kmplab.datalayer.localdatasource

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.bhanuagrawal.kmplab.datalayer.UserPagedData.UserLocalPageSource
import com.bhanuagrawal.kmplab.datalayer.UserPagedData.UserRemotePageSource
import com.bhanuagrawal.kmplab.datalayer.remotedatasource.FakeUserApi
import com.bhanuagrawal.kmplab.di.ShareModule
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.UserRepository
import com.bhanuagrawal.kmplab.domain.paging.DefaultCursorForwardPager
import com.bhanuagrawal.kmplab.domain.paging.Pager
import com.bhanuagrawal.kmplab.domain.paging.PagerConfig

fun buildSharedModule(context: Context): ShareModule{
    return ShareModule(appDatabaseBuilder(context))
}

fun appDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("app_database.db")

    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}