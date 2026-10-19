package com.bhanuagrawal.kmplab.datalayer.UserPagedData

import com.bhanuagrawal.kmplab.datalayer.remotedatasource.FakeUserApi
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.paging.PageRequest
import com.bhanuagrawal.kmplab.domain.paging.RemotePageResult
import com.bhanuagrawal.kmplab.domain.paging.RemotePageSource

class UserRemotePageSource(
    val api: FakeUserApi
): RemotePageSource<String, UserDomainModel> {


    override suspend fun getPage(pageRequest: PageRequest<String>): RemotePageResult<UserDomainModel> {

        val result = api.getUsersAfter(pageRequest.key, pageRequest.limit)
        return RemotePageResult.Success(
            data = result.users.map { it.toDomainModel() },
            nextKey = result.nextCursor,
            prevKey = result.users.firstOrNull()?.id,
            reachedEnd = result.nextCursor == null
        )
    }
}

private fun FakeUserApi.ApiUser.toDomainModel() = with(this){
    UserDomainModel(username = username, userId = id, password = "")
}
