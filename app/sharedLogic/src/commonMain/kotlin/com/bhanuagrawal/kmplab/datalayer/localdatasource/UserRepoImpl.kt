package com.bhanuagrawal.kmplab.datalayer.localdatasource

import com.bhanuagrawal.kmplab.domain.LoadDirection
import com.bhanuagrawal.kmplab.domain.UserDataRequest
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepoImpl(
    val localDataSource: LocalDataSource
): UserRepository {
    override suspend fun register(user: UserDomainModel): Result<UserDomainModel> {
        val result =  localDataSource.saveUser(user.toDataModel())
        return if(result.isSuccess && result.getOrNull() != null){
            Result.success(result.getOrNull()!!.toDomoinModel())
        }else{
            Result.failure(result.exceptionOrNull()?: Exception("Something Went wrong"))
        }
    }

    override suspend fun remove(userId: String): Result<Boolean> {
        val result =  localDataSource.remove(userId)
        return if(result.isSuccess){
            Result.success(true)
        }else{
            Result.failure(result.exceptionOrNull()?: Exception("Something Went wrong"))
        }
    }

    override suspend fun getUserRange(userDataRequest: UserDataRequest): Result<List<UserDomainModel>> {

        return when(userDataRequest.direction){
            LoadDirection.BEFORE ->{
                 localDataSource.getUserBefore(
                    userDataRequest.userId,
                    userDataRequest.limit
                ).map{ data ->
                    data.reversed().map { it.toDomoinModel() }
                }
            }
            LoadDirection.AFTER ->{
                localDataSource.getUserAfter(
                    userDataRequest.userId,
                    userDataRequest.limit
                ).map{ data ->
                    data.map { it.toDomoinModel() }
                }
            }
        }
    }


    override fun getUsers(): Flow<List<UserDomainModel>> {
        return localDataSource.getUsers()
            .map { it.map { userDataModel -> userDataModel.toDomoinModel() } }
    }
}



fun UserDomainModel.toDataModel() = UserDataModel(
    this.userId,
    this.username,
    this.password
)



fun UserDataModel.toDomoinModel() = UserDomainModel(
    this.userId,
    this.username,
    this.password
)
