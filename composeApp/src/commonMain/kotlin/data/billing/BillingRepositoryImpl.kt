package data.billing

import data.auth.AuthManager
import data.remote.AiUsageApiRepository
import domain.billing.BillingProductUiModel
import domain.billing.BillingRepository
import domain.billing.EntitlementSnapshot
import domain.billing.PurchaseHistoryItem
import domain.billing.PurchaseHistoryPage
import domain.billing.PurchaseVerificationResult
import domain.billing.RestorePurchasesResult

class BillingRepositoryImpl(
    private val apiRepository: BillingApiRepository,
    private val usageApiRepository: AiUsageApiRepository,
    private val authManager: AuthManager,
    private val platformStore: PlatformBillingStore
) : BillingRepository {

    private val storePlatform: BillingStorePlatform
        get() = PlatformBillingConfig.platform

    override suspend fun getProducts(productCodes: List<String>): List<BillingProductUiModel> {
        val catalog = apiRepository.fetchProducts()
        val filtered = if (productCodes.isEmpty()) catalog else catalog.filter { it.code in productCodes }
        val storeIds = filtered.mapNotNull { product -> storeProductId(product) }
        val storeProducts = platformStore.queryProducts(storeIds)
        return filtered.map { product ->
            val storeId = storeProductId(product)
            val storeProduct = storeProducts.firstOrNull { it.productId == storeId }
            BillingProductUiModel(
                code = product.code,
                type = product.type,
                name = product.name,
                description = product.description,
                tokenAmount = product.tokenAmount,
                tierCode = product.tierCode,
                dailyPointLimit = product.dailyPointLimit,
                storeProductId = storeId,
                formattedPrice = storeProduct?.formattedPrice
            )
        }
    }

    override suspend fun purchase(productCode: String): PurchaseVerificationResult {
        val catalog = apiRepository.fetchProducts()
        val product = catalog.firstOrNull { it.code == productCode }
            ?: return PurchaseVerificationResult(false, productCode, errorMessage = "Unknown product")
        val storeProductId = storeProductId(product)
            ?: return PurchaseVerificationResult(false, productCode, errorMessage = "Missing store product id")

        val userId = authManager.getUser()?.id
            ?: return PurchaseVerificationResult(false, productCode, errorMessage = "Not authenticated")
        val obfuscatedAccountId = platformStore.obfuscatedAccountId(userId)

        val purchase = platformStore.launchPurchase(storeProductId, obfuscatedAccountId)
            ?: return PurchaseVerificationResult(false, productCode, errorMessage = "Purchase cancelled")

        return try {
            val verified = when (storePlatform) {
                BillingStorePlatform.GOOGLE_PLAY -> apiRepository.verifyGooglePlayPurchase(
                    productCode = productCode,
                    productId = storeProductId,
                    purchaseToken = purchase.purchaseToken
                )
                BillingStorePlatform.APPLE_APP_STORE -> apiRepository.verifyApplePurchase(
                    productCode = productCode,
                    productId = storeProductId,
                    signedTransactionInfo = purchase.purchaseToken,
                    transactionId = purchase.transactionId,
                    originalTransactionId = purchase.originalTransactionId
                )
            }
            if (verified.shouldConsume || verified.shouldAcknowledge) {
                finishStorePurchase(purchase, verified.shouldConsume, verified.shouldAcknowledge)
            }
            PurchaseVerificationResult(
                success = true,
                productCode = productCode,
                purchasedTokenBalance = verified.purchasedTokenBalance,
                subscriptionTier = verified.subscriptionTier,
                shouldConsume = verified.shouldConsume
            )
        } catch (e: Exception) {
            PurchaseVerificationResult(false, productCode, errorMessage = e.message)
        }
    }

    override suspend fun restorePurchases(): RestorePurchasesResult {
        val pending = platformStore.queryPendingPurchases()
        if (pending.isEmpty()) {
            return RestorePurchasesResult(restoredCount = 0)
        }
        val catalog = apiRepository.fetchProducts()
        return try {
            val restored = when (storePlatform) {
                BillingStorePlatform.GOOGLE_PLAY -> {
                    val items = pending.mapNotNull { purchase ->
                        val product = catalog.firstOrNull { it.googlePlayProductId == purchase.productId }
                            ?: return@mapNotNull null
                        GooglePlayVerifyRequestDto(
                            productCode = product.code,
                            productId = purchase.productId,
                            purchaseToken = purchase.purchaseToken,
                            packageName = "com.setiker.app"
                        )
                    }
                    apiRepository.restorePurchases(googlePlay = items)
                }
                BillingStorePlatform.APPLE_APP_STORE -> {
                    val items = pending.mapNotNull { purchase ->
                        val product = catalog.firstOrNull { it.appleProductId == purchase.productId }
                            ?: return@mapNotNull null
                        AppleVerifyRequestDto(
                            productCode = product.code,
                            productId = purchase.productId,
                            signedTransactionInfo = purchase.purchaseToken,
                            transactionId = purchase.transactionId,
                            originalTransactionId = purchase.originalTransactionId
                        )
                    }
                    apiRepository.restorePurchases(apple = items)
                }
            }
            RestorePurchasesResult(restoredCount = restored)
        } catch (e: Exception) {
            RestorePurchasesResult(restoredCount = 0, errorMessage = e.message)
        }
    }

    override suspend fun getCurrentEntitlement(): EntitlementSnapshot {
        val usage = usageApiRepository.getUsage()
        val subscription = apiRepository.getSubscriptionSnapshot()
        return EntitlementSnapshot(
            subscriptionTier = usage.subscriptionTier,
            purchasedTokenBalance = usage.purchasedTokenBalance,
            effectiveDailyLimit = usage.effectiveDailyLimit,
            subscriptionActive = subscription.active
        )
    }

    override suspend fun recoverPendingPurchases() {
        restorePurchases()
    }

    override suspend fun getPurchaseHistory(limit: Int, offset: Int): PurchaseHistoryPage {
        val page = apiRepository.listPurchases(limit = limit, offset = offset)
        return PurchaseHistoryPage(
            purchases = page.purchases.map { dto ->
                PurchaseHistoryItem(
                    id = dto.id,
                    productCode = dto.productCode,
                    productName = dto.productName,
                    status = dto.status,
                    type = dto.type,
                    tokenAmount = dto.tokenAmount,
                    provider = dto.provider,
                    createdAt = dto.createdAt
                )
            },
            hasMore = page.hasMore
        )
    }

    private fun storeProductId(product: BillingProductDto): String? {
        return when (storePlatform) {
            BillingStorePlatform.GOOGLE_PLAY -> product.googlePlayProductId
            BillingStorePlatform.APPLE_APP_STORE -> product.appleProductId
        }
    }

    private suspend fun finishStorePurchase(
        purchase: PlatformPurchase,
        shouldConsume: Boolean,
        shouldAcknowledge: Boolean
    ) {
        val finishKey = when (storePlatform) {
            BillingStorePlatform.GOOGLE_PLAY -> purchase.purchaseToken
            BillingStorePlatform.APPLE_APP_STORE -> purchase.transactionId ?: purchase.purchaseToken
        }
        if (shouldConsume) {
            platformStore.consumePurchase(finishKey)
        }
        if (shouldAcknowledge) {
            platformStore.acknowledgePurchase(finishKey)
        }
    }
}

data class PlatformStoreProduct(
    val productId: String,
    val formattedPrice: String?
)

data class PlatformPurchase(
    val productId: String,
    val purchaseToken: String,
    val transactionId: String? = null,
    val originalTransactionId: String? = null
)

expect class PlatformBillingStore {
    suspend fun queryProducts(productIds: List<String>): List<PlatformStoreProduct>
    suspend fun launchPurchase(productId: String, obfuscatedAccountId: String): PlatformPurchase?
    suspend fun queryPendingPurchases(): List<PlatformPurchase>
    suspend fun consumePurchase(purchaseToken: String)
    suspend fun acknowledgePurchase(purchaseToken: String)
    fun obfuscatedAccountId(userId: String): String
}
