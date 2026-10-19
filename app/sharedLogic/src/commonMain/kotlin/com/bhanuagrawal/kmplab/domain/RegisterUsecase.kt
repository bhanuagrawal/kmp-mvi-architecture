package com.bhanuagrawal.kmplab.domain

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RegisterUsecase(private val userRepository: UserRepository) {

    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    suspend fun execute(username: String, password: String): Result<UserDomainModel> {
        return userRepository.register(
            UserDomainModel(
                Clock.System.now().toString() + Uuid.random().toString(),
                username,
                password
            )
        )
    }
}