package presentation.purchasehistory

import domain.billing.BillingProductUiModel
import domain.billing.BillingRepository
import domain.billing.EntitlementSnapshot
import domain.billing.PurchaseHistoryItem
import domain.billing.PurchaseHistoryPage
import domain.billing.PurchaseVerificationResult
import domain.billing.RestorePurchasesResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PurchaseHistoryViewModelTest {
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
    fun loadFirstPageSetsItemsAndHasMore() = runTest {
        val repo = FakeBillingRepository(
            pages = mapOf(
                0 to PurchaseHistoryPage(
                    purchases = (1..20).map { sampleItem("p$it") },
                    hasMore = true
                )
            )
        )

        val viewModel = PurchaseHistoryViewModel(repo)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(20, viewModel.state.value.items.size)
        assertEquals(20, viewModel.state.value.offset)
        assertTrue(viewModel.state.value.hasMore)
        assertEquals(1, repo.callCount)
    }

    @Test
    fun loadMoreAppendsNextPage() = runTest {
        val repo = FakeBillingRepository(
            pages = mapOf(
                0 to PurchaseHistoryPage(
                    purchases = (1..20).map { sampleItem("p$it") },
                    hasMore = true
                ),
                20 to PurchaseHistoryPage(
                    purchases = (21..30).map { sampleItem("p$it") },
                    hasMore = false
                )
            )
        )

        val viewModel = PurchaseHistoryViewModel(repo)
        advanceUntilIdle()

        viewModel.onIntent(PurchaseHistoryIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(30, viewModel.state.value.items.size)
        assertEquals(30, viewModel.state.value.offset)
        assertFalse(viewModel.state.value.hasMore)
        assertFalse(viewModel.state.value.isLoadingMore)
        assertEquals(2, repo.callCount)
        assertEquals("p1", viewModel.state.value.items.first().id)
        assertEquals("p30", viewModel.state.value.items.last().id)
    }

    @Test
    fun loadMoreDoesNothingWhenNoMore() = runTest {
        val repo = FakeBillingRepository(
            pages = mapOf(
                0 to PurchaseHistoryPage(
                    purchases = listOf(sampleItem("p1")),
                    hasMore = false
                )
            )
        )

        val viewModel = PurchaseHistoryViewModel(repo)
        advanceUntilIdle()

        viewModel.onIntent(PurchaseHistoryIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(1, repo.callCount)
    }

    private fun sampleItem(id: String) = PurchaseHistoryItem(
        id = id,
        productCode = "tokens_100",
        productName = "100 tokens",
        status = "completed",
        type = "consumable",
        tokenAmount = 100,
        provider = "google_play",
        createdAt = "2026-01-01T00:00:00.000Z"
    )

    private class FakeBillingRepository(
        private val pages: Map<Int, PurchaseHistoryPage>
    ) : BillingRepository {
        var callCount = 0
            private set

        override suspend fun getProducts(productCodes: List<String>): List<BillingProductUiModel> = emptyList()
        override suspend fun purchase(productCode: String): PurchaseVerificationResult =
            PurchaseVerificationResult(false, productCode)
        override suspend fun restorePurchases(): RestorePurchasesResult = RestorePurchasesResult(0)
        override suspend fun getCurrentEntitlement(): EntitlementSnapshot =
            EntitlementSnapshot("free", 0, 100, false)
        override suspend fun recoverPendingPurchases() = Unit

        override suspend fun getPurchaseHistory(limit: Int, offset: Int): PurchaseHistoryPage {
            callCount++
            return pages[offset] ?: PurchaseHistoryPage(emptyList(), hasMore = false)
        }
    }
}
