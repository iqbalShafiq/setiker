package presentation.blocked

import data.remote.ExploreApiRepository
import data.remote.model.BlockedUserItem
import data.remote.model.BlockedUsersData
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BlockedUsersViewModelTest {
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
    fun loadSuccessShowsUsers() = runTest {
        val exploreApiRepository = mockk<ExploreApiRepository>()
        coEvery { exploreApiRepository.listBlockedUsers() } returns BlockedUsersData(
            users = listOf(
                BlockedUserItem(id = "u2", username = "bob", displayName = "Bob")
            ),
            blockedUserIds = listOf("u2")
        )

        val viewModel = BlockedUsersViewModel(exploreApiRepository)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(1, viewModel.state.value.users.size)
        assertEquals("bob", viewModel.state.value.users.first().username)
    }

    @Test
    fun loadFailureSetsLoadFailed() = runTest {
        val exploreApiRepository = mockk<ExploreApiRepository>()
        coEvery { exploreApiRepository.listBlockedUsers() } throws RuntimeException("network")

        val viewModel = BlockedUsersViewModel(exploreApiRepository)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.loadFailed)
        assertTrue(viewModel.state.value.users.isEmpty())
    }

    @Test
    fun confirmUnblockRemovesUserAndEmitsMessage() = runTest {
        val exploreApiRepository = mockk<ExploreApiRepository>()
        coEvery { exploreApiRepository.listBlockedUsers() } returns BlockedUsersData(
            users = listOf(
                BlockedUserItem(id = "u2", username = "bob", displayName = "Bob"),
                BlockedUserItem(id = "u3", username = "carol", displayName = "Carol")
            ),
            blockedUserIds = listOf("u2", "u3")
        )
        coEvery { exploreApiRepository.unblockUser("u2") } returns Unit

        val viewModel = BlockedUsersViewModel(exploreApiRepository)
        advanceUntilIdle()

        viewModel.onIntent(BlockedUsersIntent.ShowUnblockConfirm("u2"))
        viewModel.onIntent(BlockedUsersIntent.ConfirmUnblock)
        advanceUntilIdle()

        assertEquals(listOf("u3"), viewModel.state.value.users.map { it.id })
        assertEquals(null, viewModel.state.value.unblockingUserId)
        assertTrue(viewModel.effect.first() is BlockedUsersEffect.ShowMessage)
        coVerify(exactly = 1) { exploreApiRepository.unblockUser("u2") }
    }

    @Test
    fun confirmUnblockFailureKeepsUserAndEmitsMessage() = runTest {
        val exploreApiRepository = mockk<ExploreApiRepository>()
        coEvery { exploreApiRepository.listBlockedUsers() } returns BlockedUsersData(
            users = listOf(BlockedUserItem(id = "u2", username = "bob")),
            blockedUserIds = listOf("u2")
        )
        coEvery { exploreApiRepository.unblockUser("u2") } throws RuntimeException("failed")

        val viewModel = BlockedUsersViewModel(exploreApiRepository)
        advanceUntilIdle()

        viewModel.onIntent(BlockedUsersIntent.ShowUnblockConfirm("u2"))
        viewModel.onIntent(BlockedUsersIntent.ConfirmUnblock)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.users.size)
        assertEquals(null, viewModel.state.value.unblockingUserId)
        assertTrue(viewModel.effect.first() is BlockedUsersEffect.ShowMessage)
    }
}
