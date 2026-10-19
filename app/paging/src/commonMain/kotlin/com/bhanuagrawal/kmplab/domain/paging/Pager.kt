package com.bhanuagrawal.kmplab.domain.paging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex

abstract class Pager<KEY, T>(
    protected open val pageConfig: PagerConfig,
    protected open val remotePageSource: RemotePageSource<KEY, T>,
    protected open val localPageSource: LocalPageSource<KEY, T>,
) {
    var pageState: PageState<KEY> = PageState()
    protected val mutex = Mutex()
    abstract fun getData(): Flow<List<T>>

    abstract fun loadMore(scrollDirection: ScrollDirection? = null): Flow<PageState<KEY>>
}

interface LocalPageSource<KEY, T>{

    fun getData(): Flow<List<T>>

    suspend fun insert(data: List<T>)

    suspend fun getCurrentKey(): KEY?

}

interface RemotePageSource<KEY, T>{
    suspend fun getPage(pageRequest:PageRequest<KEY>): RemotePageResult<T>
}

sealed class RemotePageResult<T>{
    data class Error(val exception: Exception): RemotePageResult<Nothing>()
    data class Success<KEY, T>(
        val data: List<T>,
        val nextKey: KEY?,
        val prevKey: KEY?,
        val mergeStrategy: MergeStrategy = MergeStrategy.APPEND,
        val reachedEnd: Boolean? = null
    ): RemotePageResult<T>()

}

enum class MergeStrategy{
    APPEND, PREPEND, REFRESH
}
sealed class ScrollDirection{
    data object UP: ScrollDirection()
    data object Down: ScrollDirection()
    data object Refresh: ScrollDirection()
}

data class PageState<KEY>(
    val nextKey: KEY? = null,
    val prevKey: KEY? = null,
    val reachedEnd: Boolean = false,
    val pageRequest: PageRequest<KEY>? = null,
    val status: PAgeRequestStatus = PAgeRequestStatus.IDLE,
    )


data class PageRequest<KEY>(
    val direction: ScrollDirection,
    val key: KEY?,
    val requestType: PageRequestPriority = PageRequestPriority.URGENT,
    val limit: Int
)

enum class PageRequestPriority{
    URGENT, BG
}

enum class PAgeRequestStatus{
    SUCCESS, ERROR, LOADING, IDLE
}

class PagerConfig private constructor(
     val pageSize: Int,
     val prefetchPageCount: Int,
     val nextLoadBuffer: Int
){


    class Builder{

        private var pageSize: Int = 5
        private var prefetchPageCount: Int = 2
        private var nextLoadBuffer: Int= pageSize/2

        fun pageSize(value: Int): Builder{
            this.pageSize = value
            return this
        }

        fun prefetchPageCount(value: Int): Builder{
            this.prefetchPageCount = value
            return this
        }

        fun nextLoadBuffer(value: Int): Builder{
            this.nextLoadBuffer = value
            return this
        }

        fun build(): PagerConfig{
            return PagerConfig(
                pageSize,
                prefetchPageCount,
                nextLoadBuffer
            )
        }

    }
}