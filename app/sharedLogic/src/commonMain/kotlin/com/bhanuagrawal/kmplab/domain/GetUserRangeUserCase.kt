package com.bhanuagrawal.kmplab.domain


class GetUserRangeUserCase(private val userRepository: UserRepository) {

    suspend operator fun invoke(request: UserDataRequest): Result<List<UserDomainModel>> {
        return userRepository.getUserRange(request)
    }
}

data class UserDataRequest(
    val userId: String,
    val direction: LoadDirection,
    val limit: Int = 6
)


enum class LoadDirection(val id: String){
    BEFORE("scroll_up"),
    AFTER("scroll_down")
}