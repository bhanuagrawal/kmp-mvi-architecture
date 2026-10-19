package com.bhanuagrawal.kmplab.domain


class DeleteUserUsecase(private val userRepository: UserRepository) {

    suspend fun execute(userId: String): Result<Boolean> {
        return userRepository.remove(userId)
    }
}