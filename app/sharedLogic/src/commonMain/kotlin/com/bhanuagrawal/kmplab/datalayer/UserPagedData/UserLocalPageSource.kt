package com.bhanuagrawal.kmplab.datalayer.UserPagedData

import com.bhanuagrawal.kmplab.datalayer.localdatasource.UserDao
import com.bhanuagrawal.kmplab.datalayer.localdatasource.toDataModel
import com.bhanuagrawal.kmplab.datalayer.localdatasource.toDomoinModel
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.paging.LocalPageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class UserLocalPageSource(
    val userDao: UserDao
): LocalPageSource<String, UserDomainModel> {

    override fun getData(): Flow<List<UserDomainModel>> {
        return userDao.getUsers()
            .map {
                it.map { userDataModel -> userDataModel.toDomoinModel() }
            }
            .flowOn(
                Dispatchers.IO
            )
    }

    override suspend fun insert(data: List<UserDomainModel>) {
        userDao.upsertUsers(data.map { it.toDataModel() })
    }

    override suspend fun getCurrentKey(): String? {
        return userDao.getMostRecentUser()?.userId
    }
}