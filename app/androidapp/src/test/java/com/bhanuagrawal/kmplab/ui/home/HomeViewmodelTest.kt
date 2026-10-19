package com.bhanuagrawal.kmplab.ui.home

import app.cash.turbine.test
import com.bhanuagrawal.kmplab.presentation.common.EvenResult
import com.bhanuagrawal.kmplab.domain.DeleteUserUsecase
import com.bhanuagrawal.kmplab.domain.RegisterUsecase
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.UserRepository
import com.bhanuagrawal.kmplab.presentation.home.HomePageEvents
import com.bhanuagrawal.kmplab.presentation.home.HomePageState
import com.bhanuagrawal.kmplab.presentation.home.LoadUsersEvenHandler
import com.bhanuagrawal.kmplab.presentation.home.model.User
import com.bhanuagrawal.kmplab.ui.home.HomePageSideEffects.ShowErrorToast
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class HomeViewmodelTest {
    private lateinit var homeViewmodel: HomeViewmodel

    private lateinit var repo: UserRepository

    private lateinit var registerUsecase: RegisterUsecase

    private lateinit var deleteUserUsecase: DeleteUserUsecase

    private lateinit var loadUsersEvenHandler: LoadUsersEvenHandler


    @BeforeEach
    fun beforeEachTest(){
        repo = mockk<UserRepository>()
        registerUsecase = RegisterUsecase(repo)
        deleteUserUsecase = DeleteUserUsecase(repo)
        loadUsersEvenHandler = mockk<LoadUsersEvenHandler>()
        homeViewmodel = HomeViewmodel(
            registerUser = registerUsecase,
            deleteUserUsecase = deleteUserUsecase,
            loadUsersEvenHandler = loadUsersEvenHandler
        )

    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test on add user failure generate error state`() = runTest{
        coEvery { repo.register(any()) }.returns(Result.failure(Exception()))
        every {
            loadUsersEvenHandler.handleEvent(any(), any())
        } returns emptyFlow()

        val stateJob = backgroundScope.launch {
            homeViewmodel.state.collect()
        }

        runCurrent()

        homeViewmodel.sideEffects.test {
            homeViewmodel.onEvent(HomePageEvents.AddUser)
            advanceUntilIdle()

            assertInstanceOf(
                ShowErrorToast::class.java,
                awaitItem()
            )
        }

        stateJob.cancel()

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `add user is appending new user to the list`() = runTest{
        coEvery { repo.register(any()) }.returns(Result.success(UserDomainModel("test","", password = "")))
        every {
            loadUsersEvenHandler.handleEvent(any(), any())
        } returns flowOf(EvenResult.TransformState<HomePageState>{copy( users = listOf(User("existing","")), isError = false, isLoading = false, errorMessage = null, allItemLoaded = false)})

        homeViewmodel.state.test {
            assertEquals(0, awaitItem().users.size)
            assertEquals(1, awaitItem().users.size)
            homeViewmodel.onEvent(HomePageEvents.AddUser)
            assertEquals(2, awaitItem().users.size)
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `concurrent add user keeps both updates`() = runTest {

        var id = 0

        coEvery { repo.register(any()) } coAnswers {
            id++
            Result.success(
                UserDomainModel(
                    username = "user$id",
                    userId = "$id",
                    password = ""
                )
            )
        }

        every {
            loadUsersEvenHandler.handleEvent(any(), any())
        } returns flowOf(
            EvenResult.TransformState<HomePageState> {
                copy(
                    users = listOf(User("existing", "0")),
                    isLoading = false,
                    isError = false,
                    errorMessage = null,
                    allItemLoaded = false
                )
            }
        )

        val collectJob = backgroundScope.launch {
            homeViewmodel.state.collect()
        }

        runCurrent() // LoadData completes

        // Fire both almost simultaneously
        launch { homeViewmodel.onEvent(HomePageEvents.AddUser) }
        launch { homeViewmodel.onEvent(HomePageEvents.AddUser) }

        advanceTimeBy(100.milliseconds)
        advanceUntilIdle()

        // 1 seeded user + 2 concurrent adds — the reducer reads live state, so nothing is lost.
        assertEquals(3, homeViewmodel.state.value.users.size)

        collectJob.cancel()
    }



    companion object{
        private lateinit var dispatcher: TestDispatcher


        @OptIn(ExperimentalCoroutinesApi::class)
        @BeforeAll
        @JvmStatic
        fun setup(){
            dispatcher = StandardTestDispatcher()

            Dispatchers.setMain(dispatcher)
        }


        @OptIn(ExperimentalCoroutinesApi::class)
        @AfterAll
        @JvmStatic
        fun teardown() {
            Dispatchers.resetMain()  // restore after tests
        }
    }

}