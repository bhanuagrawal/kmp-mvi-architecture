package com.bhanuagrawal.kmplab.presentation.common

import com.bhanuagrawal.kmplab.presentation.home.HomePageState
import kotlinx.coroutines.flow.Flow

interface EventHandler<E : Event, S : ViewState> {

    fun  handleEvent(
        event: E,
        stateProvider: () -> S
    ): Flow<EvenResult<HomePageState, SideEffect>>
}