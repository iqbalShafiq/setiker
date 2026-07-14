package presentation.settings

import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.AuthSessionCoordinator
import data.preferences.UserPreferencesRepository
import data.remote.AiUsageApiRepository
import data.remote.ApiException
import data.remote.LegalApiRepository
import domain.error.AppErrorCode
import domain.model.LegalSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
class SettingsViewModelDeleteAccountTest {
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
    fun deleteAccountSuccessEndsSessionAndEmitsAccountDeleted() = runTest {
        val authApiService = mockk<AuthApiService>()
        val authManager = mockk<AuthManager>()
        val authSessionCoordinator = mockk<AuthSessionCoordinator>(relaxed = true)
        val legalApiRepository = mockk<LegalApiRepository>()

        coEvery { legalApiRepository.getSummary() } returns legalSummary()
        coEvery { authManager.getUser() } returns null
        coEvery { authManager.isAuthenticated() } returns true
        coEvery { authManager.getValidAccessToken() } returns "access-token"
        coEvery { authApiService.deleteAccount("access-token", "Secret1!") } returns Unit
        coEvery { authSessionCoordinator.endSession() } returns Unit

        val viewModel = settingsViewModel(
            legalApiRepository = legalApiRepository,
            authApiService = authApiService,
            authManager = authManager,
            authSessionCoordinator = authSessionCoordinator,
        )

        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ShowDeleteConfirm)
        viewModel.onIntent(SettingsIntent.UpdateDeleteConfirmPassword("Secret1!"))
        viewModel.onIntent(SettingsIntent.UpdateDeleteConfirmPhrase("Delete Account"))
        viewModel.onIntent(SettingsIntent.ConfirmDeleteAccount)
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertEquals(SettingsEffect.AccountDeleted, effect)
        assertFalse(viewModel.state.value.isDeletingAccount)
        assertFalse(viewModel.state.value.showDeleteConfirm)
        assertNull(viewModel.state.value.deleteAccountError)
        coVerify(exactly = 1) { authApiService.deleteAccount("access-token", "Secret1!") }
        coVerify(exactly = 1) { authSessionCoordinator.endSession() }
    }

    @Test
    fun deleteAccountFailureSetsErrorState() = runTest {
        val authApiService = mockk<AuthApiService>()
        val authManager = mockk<AuthManager>()
        val authSessionCoordinator = mockk<AuthSessionCoordinator>(relaxed = true)
        val legalApiRepository = mockk<LegalApiRepository>()

        coEvery { legalApiRepository.getSummary() } returns legalSummary()
        coEvery { authManager.getUser() } returns null
        coEvery { authManager.isAuthenticated() } returns true
        coEvery { authManager.getValidAccessToken() } returns "access-token"
        coEvery {
            authApiService.deleteAccount("access-token", "wrong")
        } throws ApiException(code = AppErrorCode.CloudDeleteFailed, message = "Invalid password")

        val viewModel = settingsViewModel(
            legalApiRepository = legalApiRepository,
            authApiService = authApiService,
            authManager = authManager,
            authSessionCoordinator = authSessionCoordinator,
        )

        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ShowDeleteConfirm)
        viewModel.onIntent(SettingsIntent.UpdateDeleteConfirmPassword("wrong"))
        viewModel.onIntent(SettingsIntent.ConfirmDeleteAccount)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isDeletingAccount)
        assertNotNull(viewModel.state.value.deleteAccountError)
        coVerify(exactly = 0) { authSessionCoordinator.endSession() }
    }

    @Test
    fun deleteAccountWithoutTokenShowsNotAuthenticatedMessage() = runTest {
        val authManager = mockk<AuthManager>()
        val legalApiRepository = mockk<LegalApiRepository>()

        coEvery { legalApiRepository.getSummary() } returns legalSummary()
        coEvery { authManager.getUser() } returns null
        coEvery { authManager.isAuthenticated() } returns false
        coEvery { authManager.getValidAccessToken() } returns null
        coEvery { authManager.getAccessToken() } returns null

        val viewModel = settingsViewModel(
            legalApiRepository = legalApiRepository,
            authManager = authManager,
        )

        advanceUntilIdle()
        viewModel.onIntent(SettingsIntent.ShowDeleteConfirm)
        viewModel.onIntent(SettingsIntent.ConfirmDeleteAccount)

        val effect = viewModel.effect.first()
        assertTrue(effect is SettingsEffect.ShowMessage)
        assertFalse(viewModel.state.value.isDeletingAccount)
    }

    private fun settingsViewModel(
        legalApiRepository: LegalApiRepository = mockk(relaxed = true),
        aiUsageApiRepository: AiUsageApiRepository = mockk(relaxed = true),
        authManager: AuthManager = mockk(relaxed = true),
        authApiService: AuthApiService = mockk(relaxed = true),
        authSessionCoordinator: AuthSessionCoordinator = mockk(relaxed = true),
        userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true),
        googleSignInGateway: data.auth.GoogleSignInGateway = mockk(relaxed = true) {
            every { isAvailable() } returns false
        },
        appleSignInGateway: data.auth.AppleSignInGateway = mockk(relaxed = true) {
            every { isAvailable() } returns false
        },
    ): SettingsViewModel = SettingsViewModel(
        legalApiRepository = legalApiRepository,
        aiUsageApiRepository = aiUsageApiRepository,
        authManager = authManager,
        authApiService = authApiService,
        authSessionCoordinator = authSessionCoordinator,
        userPreferencesRepository = userPreferencesRepository,
        googleSignInGateway = googleSignInGateway,
        appleSignInGateway = appleSignInGateway,
    )

    private fun legalSummary(): LegalSummary = LegalSummary(
        privacyUrl = "https://example.com/privacy",
        termsUrl = "https://example.com/terms",
        retentionUrl = "https://example.com/retention",
        accountDeletionUrl = "https://example.com/account-deletion",
        version = "2026-07-07",
        effectiveDate = "2026-07-07",
    )
}
