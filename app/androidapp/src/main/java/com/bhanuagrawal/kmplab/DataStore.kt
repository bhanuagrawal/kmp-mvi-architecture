package com.bhanuagrawal.kmplab

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore.kt — top level, NOT inside the class
val Context.ds by preferencesDataStore(name = "global")


class AppDataStoreImpl(val appContext: Application) : AppDataStore {

    override suspend fun <T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> {
                val _key = stringPreferencesKey(key)
                appContext.ds.data.map { prefs ->
                    prefs[_key] ?: defaultValue
                }.first() as T
            }
            is Int -> {
                val _key = intPreferencesKey(key)
                appContext.ds.data.map { prefs ->
                    prefs[_key] ?: defaultValue
                }.first() as T
            }
            is Boolean -> {
                val _key = booleanPreferencesKey(key)
                appContext.ds.data.map { prefs ->
                    prefs[_key] ?: defaultValue
                }.first() as T
            }
            is Long -> {
                val _key = longPreferencesKey(key)
                appContext.ds.data.map { prefs ->
                    prefs[_key] ?: defaultValue
                }.first() as T
            }
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }

    override suspend fun <T> put(key: String, value: T) {
        when (value) {
            is String -> {
                val _key = stringPreferencesKey(key)
                appContext.ds.edit { it[_key] = value }
            }
            is Int -> {
                val _key = intPreferencesKey(key)
                appContext.ds.edit { it[_key] = value }
            }
            is Boolean -> {
                val _key = booleanPreferencesKey(key)
                appContext.ds.edit { it[_key] = value }
            }
            is Long -> {
                val _key = longPreferencesKey(key)
                appContext.ds.edit { it[_key] = value }
            }
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }
}