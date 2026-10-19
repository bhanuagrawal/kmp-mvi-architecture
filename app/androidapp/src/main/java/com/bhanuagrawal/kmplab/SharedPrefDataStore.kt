package com.bhanuagrawal.kmplab

import android.app.Application
import android.content.Context


class SharedPrefDataStore(
    appContext: Application
) : AppDataStore {

    private val privateVariable = 2
    companion object {
        const val GLOBAL_FILE = "global"
    }

    private val sharedPreferences =
        appContext.getSharedPreferences(
            GLOBAL_FILE,
            Context.MODE_PRIVATE
        )

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> get(
        key: String,
        defaultValue: T
    ): T {

        return when (defaultValue) {

            is String -> {
                sharedPreferences.getString(
                    key,
                    defaultValue
                ) as T
            }

            is Int -> {
                sharedPreferences.getInt(
                    key,
                    defaultValue
                ) as T
            }

            is Boolean -> {
                sharedPreferences.getBoolean(
                    key,
                    defaultValue
                ) as T
            }

            is Float -> {
                sharedPreferences.getFloat(
                    key,
                    defaultValue
                ) as T
            }

            is Long -> {
                sharedPreferences.getLong(
                    key,
                    defaultValue
                ) as T
            }

            is Set<*> -> {
                sharedPreferences.getStringSet(
                    key,
                    defaultValue as Set<String>
                ) as T
            }

            else -> {
                throw IllegalArgumentException(
                    "Unsupported type: ${defaultValue!!::class.java}"
                )
            }
        }
    }

    override suspend fun <T> put(
        key: String,
        value: T
    ) {

        with(sharedPreferences.edit()) {

            when (value) {

                is String -> putString(key, value)

                is Int -> putInt(key, value)

                is Boolean -> putBoolean(key, value)

                is Float -> putFloat(key, value)

                is Long -> putLong(key, value)

                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    putStringSet(
                        key,
                        value as Set<String>
                    )
                }

                else -> {
                    throw IllegalArgumentException(
                        "Unsupported type: ${value!!::class.java}"
                    )
                }
            }

            apply()
        }
    }
}

class TestCompanion{

    private val privateVal = 2

    companion object{
        fun testMethod(){
            val obj = TestCompanion()
            val value = obj.privateVal
            println(value)
        }
    }

    object test{
        fun testMethod(){
            val obj = TestCompanion()
            val value = obj.privateVal
            print("test: privateVariable")
        }
    }
}

fun verify(){

    TestCompanion.testMethod()
    TestCompanion.test.testMethod()
}