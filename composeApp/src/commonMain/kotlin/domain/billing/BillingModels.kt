package domain.billing

data class BillingProductUiModel(
    val code: String,
    val type: String,
    val name: String,
    val description: String?,
    val tokenAmount: Int?,
    val tierCode: String?,
    val dailyPointLimit: Int?,
    val storeProductId: String?,
    val formattedPrice: String?
)

data class PurchaseVerificationResult(
    val success: Boolean,
    val productCode: String,
    val purchasedTokenBalance: Int = 0,
    val subscriptionTier: String = "free",
    val shouldConsume: Boolean = false,
    val errorMessage: String? = null
)

data class RestorePurchasesResult(
    val restoredCount: Int,
    val errorMessage: String? = null
)

data class EntitlementSnapshot(
    val subscriptionTier: String,
    val purchasedTokenBalance: Int,
    val effectiveDailyLimit: Int,
    val subscriptionActive: Boolean
)

data class PurchaseHistoryItem(
    val id: String,
    val productCode: String,
    val productName: String?,
    val status: String,
    val type: String,
    val tokenAmount: Int?,
    val provider: String,
    val createdAt: String
)

data class PurchaseHistoryPage(
    val purchases: List<PurchaseHistoryItem>,
    val hasMore: Boolean
)

interface BillingRepository {
    suspend fun getProducts(productCodes: List<String> = emptyList()): List<BillingProductUiModel>
    suspend fun purchase(productCode: String): PurchaseVerificationResult
    suspend fun restorePurchases(): RestorePurchasesResult
    suspend fun getCurrentEntitlement(): EntitlementSnapshot
    suspend fun recoverPendingPurchases()
    suspend fun getPurchaseHistory(limit: Int = 20, offset: Int = 0): PurchaseHistoryPage
}
