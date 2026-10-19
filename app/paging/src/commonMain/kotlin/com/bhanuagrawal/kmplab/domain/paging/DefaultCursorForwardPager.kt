package com.bhanuagrawal.kmplab.domain.paging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.withLock

class DefaultCursorForwardPager<KEY, T>(
    pageConfig: PagerConfig,
    remotePageSource: RemotePageSource<KEY, T>,
    localPageSource: LocalPageSource<KEY, T>
) : Pager<KEY, T>(pageConfig, remotePageSource, localPageSource) {

    override fun getData() =
        localPageSource.getData()

    override fun loadMore(scrollDirection: ScrollDirection?): Flow<PageState<KEY>> = flow {

        mutex.withLock {
            if(pageState.status == PAgeRequestStatus.LOADING) return@flow
            if (pageState.reachedEnd.not() &&
                (pageState.nextKey == null || localPageSource.getCurrentKey() == pageState.nextKey)) {

                val _scrollDirection =
                    scrollDirection ?: pageState.pageRequest?.direction ?: ScrollDirection.Refresh

                val request = if (pageState.status == PAgeRequestStatus.ERROR) {
                    pageState.pageRequest!!
                } else {
                    PageRequest(
                        direction = _scrollDirection,
                        key = pageState.nextKey?:localPageSource.getCurrentKey(),
                        limit = pageConfig.pageSize
                    )
                }
                pageState = pageState.copy(
                    pageRequest = request,
                    status = PAgeRequestStatus.LOADING
                )
                emit(pageState)

                val newPageFromRemote = remotePageSource.getPage(request)

                if (newPageFromRemote is RemotePageResult.Success<*, T>) {
                    val newPageData = newPageFromRemote.data
                    localPageSource.insert(newPageData)
                    pageState = pageState.copy(
                        reachedEnd = newPageFromRemote.reachedEnd ?: newPageData.isEmpty(),
                        prevKey = newPageFromRemote.prevKey as KEY?,
                        nextKey = newPageFromRemote.nextKey as KEY?,
                        status = PAgeRequestStatus.SUCCESS
                    )
                } else {
                    pageState = pageState.copy(
                        status = PAgeRequestStatus.ERROR,
                    )
                }
                emit(pageState)
            }
        }


    }.flowOn(Dispatchers.IO)

}