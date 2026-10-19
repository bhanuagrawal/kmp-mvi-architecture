package com.bhanuagrawal.kmplab.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhanuagrawal.kmplab.presentation.common.EvenResult
import com.bhanuagrawal.kmplab.presentation.common.EvenResult.*
import com.bhanuagrawal.kmplab.presentation.common.SideEffect
import com.bhanuagrawal.kmplab.domain.DeleteUserUsecase
import com.bhanuagrawal.kmplab.domain.RegisterUsecase
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.paging.ScrollDirection
import com.bhanuagrawal.kmplab.presentation.common.Event
import com.bhanuagrawal.kmplab.presentation.common.ViewState
import com.bhanuagrawal.kmplab.presentation.home.HomePageEvents
import com.bhanuagrawal.kmplab.presentation.home.HomePageState
import com.bhanuagrawal.kmplab.presentation.home.LoadUsersEvenHandler
import com.bhanuagrawal.kmplab.presentation.home.model.User
import com.bhanuagrawal.kmplab.ui.home.HomePageSideEffects.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeViewmodel(
    val registerUser: RegisterUsecase,
    val deleteUserUsecase: DeleteUserUsecase,
    val loadUsersEvenHandler: LoadUsersEvenHandler
) : ViewModel() {


    private val events = MutableSharedFlow<HomePageEvents>(extraBufferCapacity = 1)
    private val _sideEffects = MutableSharedFlow<SideEffect>()
    val sideEffects = _sideEffects

    @OptIn(FlowPreview::class)
    val addUserEvent: SharedFlow<HomePageEvents.AddUser> =
        events.filterIsInstance<HomePageEvents.AddUser>().debounce(100.milliseconds)
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
    private val initialState = HomePageState(
        users = emptyList(),
        isError = false,
        isLoading = false,
        errorMessage = null,
        allItemLoaded = false
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val state = events.onStart {
            onEvent(HomePageEvents.LoadData())
        }
        .flatMapMerge {
            handleEvent(it)
        }
        .onEach {
            if(it is SideEffectEvent){
                _sideEffects.emit(it.sideEffect)
            }
        }
        .filterIsInstance<TransformState<HomePageState>>()
        // Fold each reducer over the LATEST accumulated state — the transform
        // runs against live state at reduction time, not a stale snapshot.
        .scan(initialState) { state, result -> result.transform(state) }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState
        )

    private fun handleEvent(event: HomePageEvents): Flow<EvenResult<HomePageState, SideEffect>> {

        return when (event) {

            is HomePageEvents.AddUser -> {
                flow {
                    val newUserResult = registerUser.execute(
                        System.currentTimeMillis().toString() + "@gmail.com", "123"
                    )
                    if (newUserResult.isFailure) {
                        emit(
                            SideEffectEvent(
                                ShowErrorToast("Could not add User")
                            )
                        )
                    } else {
                        val added = newUserResult.getOrNull()!!

                        // Artificial suspension — proves the reducer is race-safe:
                        // it reads `users` from the live state at reduction time, not a snapshot.
                        delay(100.milliseconds)

                        emit(
                            TransformState<HomePageState> {
                                copy(users = users + User(username = added.username, userId = added.userId))
                            }
                        )
                    }


                }
            }

            is HomePageEvents.LoadData -> {
                loadUsersEvenHandler.handleEvent(event) {
                    state.value
                }
            }

            is HomePageEvents.RemoveUser -> {

                flow {
                    val newUserResult = deleteUserUsecase.execute(event.user.userId)
                    if (newUserResult.isFailure) {
                        emit(
                            SideEffectEvent(
                                ShowErrorToast("Could not delete User")
                            )
                        )
                    } else {
                        emit(
                            TransformState<HomePageState> {
                                copy(users = users.filter { it.userId != event.user.userId })
                            }
                        )
                    }

                }
            }
        }

    }

    private fun getUserUIModel(userDomainModel: UserDomainModel): User {
        return User(
            userId = userDomainModel.userId, username = userDomainModel.username
        )
    }


    fun onEvent(event: HomePageEvents) {
        viewModelScope.launch {
            events.emit(event)
        }
    }
}






sealed class HomePageSideEffects : SideEffect {
    data class ShowErrorToast(val message: String) : HomePageSideEffects()
}

