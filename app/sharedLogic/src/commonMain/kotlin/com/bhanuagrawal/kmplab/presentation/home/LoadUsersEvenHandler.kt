package com.bhanuagrawal.kmplab.presentation.home

import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.paging.PAgeRequestStatus
import com.bhanuagrawal.kmplab.domain.paging.Pager
import com.bhanuagrawal.kmplab.domain.paging.ScrollDirection
import com.bhanuagrawal.kmplab.presentation.common.EvenResult
import com.bhanuagrawal.kmplab.presentation.common.EventHandler
import com.bhanuagrawal.kmplab.presentation.common.SideEffect
import com.bhanuagrawal.kmplab.presentation.home.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class LoadUsersEvenHandler(
    private val pager: Pager<String, UserDomainModel>
) : EventHandler<HomePageEvents.LoadData, HomePageState> {

    override fun handleEvent(
        event: HomePageEvents.LoadData,
        stateProvider: () -> HomePageState
    ): Flow<EvenResult<HomePageState, SideEffect>> = flow {

        if (event.scrollDirection == null) {

            pager.getData().collect { userDomainModels ->

                emit(
                    EvenResult.TransformState<HomePageState> {
                        copy(
                            users = userDomainModels.map {
                                User(it.username, it.userId)
                            }
                        )
                    }
                )

                if (userDomainModels.isEmpty() && !pager.pageState.reachedEnd) {
                    loadMore(ScrollDirection.Refresh).collect {
                        emit(it)
                    }
                }
            }

        } else {

            // Decision based on current state → stateProvider is still fine here
            if (stateProvider().isLoading) return@flow

            emit(
                EvenResult.TransformState<HomePageState> {
                    copy(isLoading = true)
                }
            )

            loadMore(ScrollDirection.Refresh).collect {
                emit(it)
            }
        }

    }.flowOn(Dispatchers.IO)

    fun loadMore(
        scrollDirection: ScrollDirection
    ): Flow<EvenResult<HomePageState, SideEffect>> = flow {

        pager.loadMore(scrollDirection).collect {

            emit(
                EvenResult.TransformState {
                    copy(
                        isLoadingAfter = it.status == PAgeRequestStatus.LOADING,
                        isLoading = it.status == PAgeRequestStatus.LOADING
                    )
                }
            )
        }
    }
}