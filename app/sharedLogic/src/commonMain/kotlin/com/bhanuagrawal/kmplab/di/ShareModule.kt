package com.bhanuagrawal.kmplab.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bhanuagrawal.kmplab.datalayer.UserPagedData.UserLocalPageSource
import com.bhanuagrawal.kmplab.datalayer.UserPagedData.UserRemotePageSource
import com.bhanuagrawal.kmplab.datalayer.localdatasource.AppDatabase
import com.bhanuagrawal.kmplab.datalayer.localdatasource.LocalDBDataSource
import com.bhanuagrawal.kmplab.datalayer.localdatasource.LocalDataSource
import com.bhanuagrawal.kmplab.datalayer.localdatasource.UserDao
import com.bhanuagrawal.kmplab.datalayer.localdatasource.UserRepoImpl
import com.bhanuagrawal.kmplab.datalayer.remotedatasource.FakeUserApi
import com.bhanuagrawal.kmplab.domain.DeleteUserUsecase
import com.bhanuagrawal.kmplab.domain.RegisterUsecase
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.UserRepository
import com.bhanuagrawal.kmplab.domain.paging.DefaultCursorForwardPager
import com.bhanuagrawal.kmplab.domain.paging.Pager
import com.bhanuagrawal.kmplab.domain.paging.PagerConfig

class ShareModule(val builder: RoomDatabase.Builder<AppDatabase>){


    fun buildDatabase(): AppDatabase =
        builder.setDriver(BundledSQLiteDriver()).build()
    fun getUserDao(): UserDao{
        return buildDatabase().userDao()
    }

    fun getLocalDataSource(): LocalDataSource{
        return LocalDBDataSource(getUserDao())
    }
    fun getUserRepo(): UserRepository{
        return UserRepoImpl(getLocalDataSource())
    }


    fun getRegisterUsecase(): RegisterUsecase{
        return RegisterUsecase(getUserRepo())
    }

    fun getDeleteUserUsecase(): DeleteUserUsecase{
        return DeleteUserUsecase(getUserRepo())
    }


    fun getUserPager(): Pager<String, UserDomainModel> {
        val userPager = DefaultCursorForwardPager(
            pageConfig = PagerConfig.Builder().pageSize(5).build(),
            remotePageSource = UserRemotePageSource(FakeUserApi()),
            localPageSource = UserLocalPageSource(getUserDao())
        )

        return userPager
    }

}