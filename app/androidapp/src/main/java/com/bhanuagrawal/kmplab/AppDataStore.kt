package com.bhanuagrawal.kmplab

interface AppDataStore {

    suspend fun <T> get(
        key: String,
        defaultValue: T
    ): T

    suspend fun <T> put(
        key: String,
        value: T
    )
}