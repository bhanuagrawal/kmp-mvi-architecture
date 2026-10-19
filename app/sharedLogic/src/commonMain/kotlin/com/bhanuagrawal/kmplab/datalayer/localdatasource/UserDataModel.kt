package com.bhanuagrawal.kmplab.datalayer.localdatasource

import androidx.room3.Entity
import androidx.room3.PrimaryKey


@Entity(tableName = "users")
class UserDataModel (
    @PrimaryKey
    val userId: String,
    val username: String,
    val password: String
)