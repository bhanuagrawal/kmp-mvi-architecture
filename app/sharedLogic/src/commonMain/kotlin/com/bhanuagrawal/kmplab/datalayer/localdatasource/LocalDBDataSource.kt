package com.bhanuagrawal.kmplab.datalayer.localdatasource

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LocalDBDataSource(val userDao: UserDao) : LocalDataSource {
    override suspend fun saveUser(user: UserDataModel): Result<UserDataModel> {
        return  withContext(Dispatchers.IO){
            try{
                userDao.insert(user)
                Result.success(user)
            }catch (c: CancellationException){
                throw c
            }catch (e: Exception){
                Result.failure(e)
            }

        }
    }

    override fun getUsers(): Flow<List<UserDataModel>> {
        return userDao.getUsers()

    }

    override suspend fun remove(userId: String): Result<Boolean> {
        return  withContext(Dispatchers.IO){
            try{
                userDao.remove(userId)
                Result.success(true)
            }catch (c: CancellationException){
                throw c
            }catch (e: Exception){
                Result.failure(e)
            }

        }
    }

    override suspend fun getUserAfter(userId: String?, limit: Int): Result<List<UserDataModel>> {
        return  withContext(Dispatchers.IO){
            try{
                if(userId == null){
                    Result.success(userDao.getUsersFromStart( limit))
                }else{
                    Result.success(userDao.getUserAfter(userId, limit))
                }
            }catch (c: CancellationException){
                throw c
            }catch (e: Exception){
                Result.failure(e)
            }
        }
    }

    override suspend fun getUserBefore(userId: String, limit: Int): Result<List<UserDataModel>> {
        return  withContext(Dispatchers.IO){
            try{
                Result.success(userDao.getUserBefore(userId, limit))
            }catch (c: CancellationException){
                throw c
            }catch (e: Exception){
                Result.failure(e)
            }
        }
    }
}