package com.bhanuagrawal.kmplab.ui

import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import android.os.Parcelable
import com.bhanuagrawal.kmplab.AppDataStore
import com.bhanuagrawal.kmplab.domain.RegisterUsecase
import com.bhanuagrawal.kmplab.presentation.common.EvenResult
import com.bhanuagrawal.kmplab.presentation.common.Event
import com.bhanuagrawal.kmplab.presentation.common.SideEffect
import com.bhanuagrawal.kmplab.presentation.common.ViewState
import kotlinx.coroutines.flow.filterIsInstance

class RegistrationViewmodel(
    val dataStore: AppDataStore,
    val savedStateHandle: SavedStateHandle,
    val registerUseCase: RegisterUsecase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel(){

    private val _events = MutableSharedFlow<ResistrationEvents>()
    private val _sideEffects = MutableSharedFlow<SideEffect>()
    val sideEffects = _sideEffects
    private val initialState: RegistrationState =
        savedStateHandle.get<RegistrationState>("state")
            ?: RegistrationState(isError = false, isLoading = false, errorMessage = null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state =
        _events
            .flatMapMerge{
                handleEvent(it)
            }
            .onEach {
                if(it is EvenResult.SideEffectEvent){
                    _sideEffects.emit(it.sideEffect)
                }
            }
            .filterIsInstance<EvenResult.TransformState<RegistrationState>>()
            .scan(initialState) { state, result ->
                val newState = result.transform(state)
                savedStateHandle["state"] = newState   // persist the resulting state, not the reducer
                newState
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                initialState
            )



    fun onEvent(event: ResistrationEvents){
        viewModelScope.launch {
            _events.emit(event)
        }
    }


    fun handleEvent(event: ResistrationEvents): Flow<EvenResult<RegistrationState, SideEffect>> = flow {

        when (event) {
            is ResistrationEvents.Register -> {
                emit(
                    EvenResult.TransformState<RegistrationState> {
                        copy(isLoading = true, isError = false)
                    }
                )

                val result = registerUseCase.execute(event.username, event.password)

                emit(
                    EvenResult.TransformState<RegistrationState> {
                        copy(
                            isLoading = false,
                            isError = result.isFailure,
                            userId = result.getOrNull()?.userId
                        )
                    }
                )

                if(result.isFailure){
                    emit(
                        EvenResult.SideEffectEvent(
                            RegistrationSideEffects.ShowErrorToast(result.exceptionOrNull()?.message?:"Something went wrong")
                        )
                    )
                }
                if (result.isSuccess) {
                    emit(
                        EvenResult.SideEffectEvent(
                            Navigation.Home
                        )
                    )
                }
            }

            is ResistrationEvents.Validate -> {
                val emailInvalidError = if (!event.username.contains("@")) "Invalid Email" else ""
                val passwordInvalidError =
                    if (emailInvalidError.isEmpty() && event.password.isEmpty()) "Enter Password" else ""
                val hasError = emailInvalidError.isNotEmpty() || passwordInvalidError.isNotEmpty()

                emit(
                    EvenResult.TransformState<RegistrationState> {
                        copy(
                            emailInvalidError = emailInvalidError,
                            passwordInvalidError = passwordInvalidError,
                            isError = hasError
                        )
                    }
                )
                if (hasError) {
                    emit(
                        EvenResult.SideEffectEvent(
                            RegistrationSideEffects.ShowErrorToast(
                                emailInvalidError.ifEmpty { passwordInvalidError }
                            )
                        )
                    )
                }
            }
        }
    }.flowOn(ioDispatcher)
}



data class RegistrationState(
    val userId: String? = null,
    val emailInvalidError: String? = null,
    val passwordInvalidError: String? = null,
    override val isLoading: Boolean = false,
    override val isError: Boolean = false,
    override val errorMessage: String? = null,
) : ViewState, Parcelable {

    constructor(parcel: Parcel) : this(
        userId = parcel.readString(),
        emailInvalidError = parcel.readString(),
        passwordInvalidError = parcel.readString(),
        isLoading = parcel.readByte() != 0.toByte(),
        isError = parcel.readByte() != 0.toByte(),
        errorMessage = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userId)
        parcel.writeString(emailInvalidError)
        parcel.writeString(passwordInvalidError)
        parcel.writeByte(if (isLoading) 1 else 0)
        parcel.writeByte(if (isError) 1 else 0)
        parcel.writeString(errorMessage)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RegistrationState> {
        override fun createFromParcel(parcel: Parcel): RegistrationState {
            return RegistrationState(parcel)
        }

        override fun newArray(size: Int): Array<RegistrationState?> {
            return arrayOfNulls(size)
        }
    }
}

sealed class ResistrationEvents: Event {
    data class Register(val username: String, val password: String): ResistrationEvents()
    data class Validate(val username: String, val password: String): ResistrationEvents()

}


sealed class Navigation(val screen: Screen): SideEffect {
    data object Home: Navigation(screen = Screen.HomeScreen)
}
sealed class RegistrationSideEffects: SideEffect {
    data class ShowErrorToast(val message: String): RegistrationSideEffects()
}