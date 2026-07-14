package presentation.legal

import data.auth.AuthManager
import data.remote.ApiException
import data.remote.LegalApiRepository
import domain.error.AppErrorCode
import domain.model.LegalDocument
import domain.model.LegalSection
import domain.model.User
import domain.model.UserRole
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
import presentation.common.UiText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LegalDocumentViewModelTest {
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
    fun accountDeletionLoadPrefillsEmailAndShowsForm() = runTest {
        val legalApi = mockk<LegalApiRepository>()
        val authManager = mockk<AuthManager>()
        coEvery { authManager.getUser() } returns sampleUser(email = "user@setiker.app")
        coEvery { legalApi.getAccountDeletionDocument() } returns sampleDeletionDoc()

        val viewModel = LegalDocumentViewModel(legalApi, authManager, "ACCOUNT_DELETION")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showDeletionForm)
        assertEquals("user@setiker.app", viewModel.state.value.deletionEmail)
        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.document)
    }

    @Test
    fun submitDeletionSuccessSetsSuccessMessage() = runTest {
        val legalApi = mockk<LegalApiRepository>()
        val authManager = mockk<AuthManager>()
        coEvery { authManager.getUser() } returns sampleUser()
        coEvery { legalApi.getAccountDeletionDocument() } returns sampleDeletionDoc()
        coEvery {
            legalApi.requestAccountDeletion(
                email = "user@setiker.app",
                reason = "Leaving",
                confirmed = true
            )
        } returns "Request received."

        val viewModel = LegalDocumentViewModel(legalApi, authManager, "ACCOUNT_DELETION")
        advanceUntilIdle()

        viewModel.onIntent(LegalDocumentIntent.UpdateDeletionEmail("user@setiker.app"))
        viewModel.onIntent(LegalDocumentIntent.UpdateDeletionReason("Leaving"))
        viewModel.onIntent(LegalDocumentIntent.SetDeletionConfirmed(true))
        viewModel.onIntent(LegalDocumentIntent.SubmitDeletionRequest)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSubmittingDeletion)
        assertNull(viewModel.state.value.deletionSubmitError)
        assertEquals(
            UiText.DynamicString("Request received."),
            viewModel.state.value.deletionSubmitSuccessMessage
        )
        assertFalse(viewModel.state.value.deletionConfirmed)
        coVerify(exactly = 1) {
            legalApi.requestAccountDeletion(
                email = "user@setiker.app",
                reason = "Leaving",
                confirmed = true
            )
        }
    }

    @Test
    fun submitDeletionFailureSetsError() = runTest {
        val legalApi = mockk<LegalApiRepository>()
        val authManager = mockk<AuthManager>()
        coEvery { authManager.getUser() } returns sampleUser()
        coEvery { legalApi.getAccountDeletionDocument() } returns sampleDeletionDoc()
        coEvery {
            legalApi.requestAccountDeletion(any(), any(), any())
        } throws ApiException(AppErrorCode.CloudCreateFailed, message = "Too many requests")

        val viewModel = LegalDocumentViewModel(legalApi, authManager, "ACCOUNT_DELETION")
        advanceUntilIdle()

        viewModel.onIntent(LegalDocumentIntent.SetDeletionConfirmed(true))
        viewModel.onIntent(LegalDocumentIntent.SubmitDeletionRequest)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSubmittingDeletion)
        assertEquals(
            UiText.DynamicString("Too many requests"),
            viewModel.state.value.deletionSubmitError
        )
        assertNull(viewModel.state.value.deletionSubmitSuccessMessage)
    }

    @Test
    fun submitWithoutConfirmDoesNotCallApi() = runTest {
        val legalApi = mockk<LegalApiRepository>()
        val authManager = mockk<AuthManager>()
        coEvery { authManager.getUser() } returns sampleUser()
        coEvery { legalApi.getAccountDeletionDocument() } returns sampleDeletionDoc()

        val viewModel = LegalDocumentViewModel(legalApi, authManager, "ACCOUNT_DELETION")
        advanceUntilIdle()

        viewModel.onIntent(LegalDocumentIntent.SubmitDeletionRequest)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.deletionSubmitError)
        coVerify(exactly = 0) { legalApi.requestAccountDeletion(any(), any(), any()) }
    }

    private fun sampleDeletionDoc() = LegalDocument(
        title = "Account deletion",
        summary = "How to delete your account",
        sections = listOf(
            LegalSection(id = "overview", title = "Overview", body = "You can request deletion.")
        )
    )

    private fun sampleUser(email: String = "user@setiker.app") = User(
        id = "u1",
        email = email,
        username = "user",
        name = "User",
        role = UserRole(id = "r1", name = "user"),
        isActive = true,
        createdAt = 0L
    )
}
