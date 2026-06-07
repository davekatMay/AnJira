package com.anjira.taskplanner.ui.viewmodel

import com.anjira.taskplanner.domain.model.User
import com.anjira.taskplanner.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login sets Success state on success`() = runTest(testDispatcher) {
        val user = User(1, "testuser", "test@example.com")
        coEvery { authRepository.login("test@example.com", "password") } returns user

        viewModel.login("test@example.com", "password")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthViewModel.UiState.Success)
        assertEquals(user, (state as AuthViewModel.UiState.Success).user)
    }

    @Test
    fun `login sets Error state on failure`() = runTest(testDispatcher) {
        coEvery { authRepository.login("wrong@example.com", "wrong") } throws Exception("Invalid credentials")

        viewModel.login("wrong@example.com", "wrong")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthViewModel.UiState.Error)
        assertEquals("Invalid credentials", (state as AuthViewModel.UiState.Error).message)
    }

    @Test
    fun `register sets Success state on success`() = runTest(testDispatcher) {
        val user = User(2, "newuser", "new@example.com")
        coEvery { authRepository.register("new@example.com", "password") } returns user

        viewModel.register("new@example.com", "password")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthViewModel.UiState.Success)
        assertEquals(user, (state as AuthViewModel.UiState.Success).user)
    }

    @Test
    fun `register sets Error state on failure`() = runTest(testDispatcher) {
        coEvery { authRepository.register("dup@example.com", "password") } throws Exception("Email already registered")

        viewModel.register("dup@example.com", "password")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthViewModel.UiState.Error)
        assertEquals("Email already registered", (state as AuthViewModel.UiState.Error).message)
    }

    @Test
    fun `logout calls repository logout`() = runTest(testDispatcher) {
        coEvery { authRepository.logout() } returns Unit

        viewModel.logout()
        advanceUntilIdle()

        coVerify { authRepository.logout() }
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(AuthViewModel.UiState.Idle, viewModel.uiState.value)
    }
}
