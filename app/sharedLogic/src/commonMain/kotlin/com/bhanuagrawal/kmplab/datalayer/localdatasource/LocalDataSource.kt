package com.bhanuagrawal.kmplab.datalayer.localdatasource

import kotlinx.coroutines.flow.Flow

interface LocalDataSource {
    suspend fun saveUser(user: UserDataModel): Result<UserDataModel>

    fun getUsers(): Flow<List<UserDataModel>>
    suspend fun remove(userId: String): Result<Boolean>

    suspend fun getUserAfter(userId: String?, limit: Int): Result<List<UserDataModel>>

    suspend fun getUserBefore(userId: String, limit: Int): Result<List<UserDataModel>>
}