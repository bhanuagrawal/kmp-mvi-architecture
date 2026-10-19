package com.bhanuagrawal.kmplab

import androidx.lifecycle.SavedStateHandle
import com.bhanuagrawal.kmplab.domain.RegisterUsecase
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.UserRepository
import com.bhanuagrawal.kmplab.ui.RegistrationViewmodel
import com.bhanuagrawal.kmplab.ui.ResistrationEvents
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RegistrationViewmodelTest {
    companion object{
        private lateinit var dispatcher: TestDispatcher
        private lateinit var repo: UserRepository
        private lateinit var viewModel: RegistrationViewmodel


        @OptIn(ExperimentalCoroutinesApi::class)
        @BeforeAll
        @JvmStatic
        fun setup(){
            dispatcher = StandardTestDispatcher()
            repo = mockk<UserRepository>()
            coEvery { repo.register(any()) }.returns(
                Result.success(
                    UserDomainModel(userId = "1", username = "bhanu@gmail.com", password = "123")
                )
            )

            Dispatchers.setMain(dispatcher)


            viewModel = RegistrationViewmodel(
                dataStore = mockk(),
                savedStateHandle = SavedStateHandle(),
                registerUseCase = RegisterUsecase(repo),
                // flowOn(Dispatchers.IO) would move the work off the test scheduler and make
                // every mid-flight assertion a race — point it at the test dispatcher instead.
                ioDispatcher = dispatcher
            )
        }


        @OptIn(ExperimentalCoroutinesApi::class)
        @AfterAll
        @JvmStatic
        fun teardown() {
            Dispatchers.resetMain()  // restore after tests
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `loading state is false after register event`() = runTest {

        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()  // subscribe before emitting; MutableSharedFlow has no replay buffer

        viewModel.onEvent(ResistrationEvents.Register("bhanu@gmail.com", "123"))

        advanceUntilIdle()  // runs all pending coroutines

        assertEquals(false, viewModel.state.value.isLoading)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `loading is true during registration`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { repo.register(any()) } coAnswers {
            gate.await()
            Result.success(
                UserDomainModel(userId = "1", username = "bhanu@gmail.com", password = "123")
            )
        }

        // `state` is a stateIn(WhileSubscribed) flow — without a collector it never runs.
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()  // subscribe before emitting; MutableSharedFlow has no replay buffer

        viewModel.onEvent(ResistrationEvents.Register("bhanu@gmail.com", "123"))
        advanceUntilIdle()  // isLoading = true emitted, then the use case parks on the gate

        assertEquals(true, viewModel.state.value.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()  // now finish
        assertEquals(false, viewModel.state.value.isLoading)
    }
}
