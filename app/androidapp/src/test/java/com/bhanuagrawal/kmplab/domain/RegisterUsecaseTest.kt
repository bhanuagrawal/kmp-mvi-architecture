package com.bhanuagrawal.kmplab.sharedlogic.domain

import com.bhanuagrawal.kmplab.domain.RegisterUsecase
import com.bhanuagrawal.kmplab.domain.UserDomainModel
import com.bhanuagrawal.kmplab.domain.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RegisterUsecaseTest {





    companion object{
        lateinit var repo: UserRepository
        lateinit var userCase: RegisterUsecase


        @BeforeAll
        @JvmStatic
        fun setup() {
            repo = mockk<UserRepository>()
            userCase = RegisterUsecase(repo)
            coEvery { repo.register(any()) }.returns(
                Result.success(
                    UserDomainModel(
                        userId = "1",
                        username = "bhanu@gmail.com",
                        password = "123"
                    )
                )
            )
        }
    }


    @Test
    fun testregistration() = runTest{
        val result = userCase.execute("bhanu@gmail.com", "123")
        assertEquals(
            Result.success(
                UserDomainModel(
                    userId = "1",
                    username = "bhanu@gmail.com",
                    password = "123"
                )
            ),
            result
        )
    }


    @Test
    fun `register passes a generated userId to the repository`() = runTest {
        val user = slot<UserDomainModel>()

        userCase.execute("bhanu@gmail.com", "123")

        coVerify { repo.register(capture(user)) }
        assertEquals("bhanu@gmail.com", user.captured.username)
        assertEquals("123", user.captured.password)
        assertTrue(user.captured.userId.isNotBlank())
    }



}