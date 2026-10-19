package com.bhanuagrawal.kmplab.datalayer.localdatasource

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: UserDataModel)

    @Upsert
    suspend fun upsertUsers(users: List<UserDataModel>)
    @Query("SELECT * from users")
    fun getUsers(): Flow<List<UserDataModel>>
    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun remove(userId: String)

    @Query("SELECT * from users where userId > :userId order by userId ASC LIMIT :limit")
    suspend fun getUserAfter(userId: String, limit: Int): List<UserDataModel>

    @Query("SELECT * from users where userId < :userId order by userId DESC LIMIT :limit")
    suspend fun getUserBefore(userId: String, limit: Int): List<UserDataModel>

    @Query("SELECT * from users order by userId ASC LIMIT :limit")
    suspend fun getUsersFromStart(limit: Int): List<UserDataModel>

    @Query("""
    SELECT * FROM users
    ORDER BY userId DESC
    LIMIT 1
""")
    suspend fun getMostRecentUser(): UserDataModel?
}