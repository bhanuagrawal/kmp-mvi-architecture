package com.bhanuagrawal.kmplab.domain

import kotlinx.coroutines.flow.Flow


class GetUsersUserCase(private val userRepository: UserRepository) {

    operator fun invoke(): Flow<List<UserDomainModel>> {
        return userRepository.getUsers()
    }
}