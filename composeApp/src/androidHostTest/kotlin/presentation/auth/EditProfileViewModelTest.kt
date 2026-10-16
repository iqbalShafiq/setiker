package presentation.auth

import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.model.UpdateProfileRequest
import data.auth.model.UserDto
import data.auth.model.UserProfileResponse
import data.remote.ApiException
import domain.error.AppErrorCode
import domain.model.User
import domain.model.UserRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPopulatesDisplayNameAndUsernameFromProfile() = runTest {
        val authManager = mockk<AuthManager>()
        val authApiService = mockk<AuthApiService>()
        coEvery { authManager.getUser() } returns sampleUser()
        coEvery { authManager.getValidAccessToken() } returns "token"
        coEvery { authApiService.getProfile("token") } returns UserProfileResponse(
            success = true,
            data = UserDto(
                id = "u1",
                email = "a@b.com",
                username = "alice",
                displayName = "Alice",
                role = "user",
                isActive = true,
                createdAt = "2024-01-01T00:00:00.000Z",
                totalPackDownloads = 12
            )
        )
        coEvery { authManager.saveUser(any()) } returns Unit

        val viewModel = EditProfileViewModel(authManager, authApiService)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Alice", viewModel.state.value.displayName)
        assertEquals("alice", viewModel.state.value.username)
        assertEquals("a@b.com", viewModel.state.value.email)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun saveRejectsInvalidUsernameWithoutCallingApi() = runTest {
        val authManager = mockk<AuthManager>(relaxed = true)
        val authApiService = mockk<AuthApiService>(relaxed = true)
        coEvery { authManager.getUser() } returns sampleUser()
        coEvery { authManager.getValidAccessToken() } returns "token"
        coEvery { authApiService.getProfile("token") } returns UserProfileResponse(
            success = true,
            data = null
        )

        val viewModel = EditProfileViewModel(authManager, authApiService)
        advanceUntilIdle()

        viewModel.onIntent(EditProfileIntent.UpdateUsername("ab"))
        viewModel.onIntent(EditProfileIntent.Save)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.usernameError)
        coVerify(exactly = 0) { authApiService.updateProfile(any(), any()) }
    }

    @Test
    fun saveSuccessPersistsUserAndNavigatesBack() = runTest {
        val authManager = mockk<AuthManager>()
        val authApiService = mockk<AuthApiService>()
        coEvery { authManager.getUser() } returns sampleUser()
        coEvery { authManager.getValidAccessToken() } returns "token"
        coEvery { authApiService.getProfile("token") } returns UserProfileResponse(
            success = true,
            data = null
        )
        coEvery {
            authApiService.updateProfile(
                token = "token",
                request = UpdateProfileRequest(displayName = "New Name", username = "new_user")
            )
        } returns UserProfileResponse(
            success = true,
            data = UserDto(
                id = "u1",
                email = "a@b.com",
                username = "new_user",
                displayName = "New Name",
                role = "user",
                isActive = true,
                createdAt = "2024-01-01T00:00:00.000Z"
            )
        )
        coEvery { authManager.saveUser(any()) } returns Unit

        val viewModel = EditProfileViewModel(authManager, authApiService)
        advanceUntilIdle()

        viewModel.onIntent(EditProfileIntent.UpdateDisplayName("New Name"))
        viewModel.onIntent(EditProfileIntent.UpdateUsername("new_user"))
        viewModel.onIntent(EditProfileIntent.Save)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSaving)
        assertNull(viewModel.state.value.error)
        coVerify(exactly = 1) { authManager.saveUser(any()) }
        assertTrue(viewModel.effect.first() is EditProfileEffect.ShowMessage)
    }

    @Test
    fun saveUsernameTakenSetsUsernameError() = runTest {
        val authManager = mockk<AuthManager>()
        val authApiService = mockk<AuthApiService>()
        coEvery { authManager.getUser() } returns sampleUser()
        coEvery { authManager.getValidAccessToken() } returns "token"
        coEvery { authApiService.getProfile("token") } returns UserProfileResponse(
            success = true,
            data = null
        )
        coEvery {
            authApiService.updateProfile(any(), any())
        } throws ApiException(code = AppErrorCode.AuthUsernameTaken, message = "taken")

        val viewModel = EditProfileViewModel(authManager, authApiService)
        advanceUntilIdle()

        viewModel.onIntent(EditProfileIntent.UpdateUsername("taken_name"))
        viewModel.onIntent(EditProfileIntent.Save)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSaving)
        assertNotNull(viewModel.state.value.usernameError)
        assertNotNull(viewModel.state.value.error)
    }

    private fun sampleUser() = User(
        id = "u1",
        email = "a@b.com",
        username = "alice",
        name = "Alice",
        role = UserRole(id = "r1", name = "user"),
        isActive = true,
        createdAt = 0L,
        totalPackDownloads = 12
    )
}
