package com.bhanuagrawal.kmplab.domain

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun register(user: UserDomainModel): Result<UserDomainModel>

    suspend fun  remove(userId: String): Result<Boolean>


    suspend fun getUserRange(userDataRequest: UserDataRequest): Result<List<UserDomainModel>>

    fun getUsers(): Flow<List<UserDomainModel>>

}