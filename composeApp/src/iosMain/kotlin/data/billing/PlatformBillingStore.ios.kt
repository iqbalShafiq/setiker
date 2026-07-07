package data.billing

actual class PlatformBillingStore {
    actual suspend fun queryProducts(productIds: List<String>): List<PlatformStoreProduct> {
        return IosStoreKitIntegration.queryProducts(productIds)
    }

    actual suspend fun launchPurchase(productId: String, obfuscatedAccountId: String): PlatformPurchase? {
        return IosStoreKitIntegration.purchase(productId)
    }

    actual suspend fun queryPendingPurchases(): List<PlatformPurchase> {
        return IosStoreKitIntegration.queryPendingPurchases()
    }

    actual suspend fun consumePurchase(purchaseToken: String) {
        IosStoreKitIntegration.finishPurchase(purchaseToken)
    }

    actual suspend fun acknowledgePurchase(purchaseToken: String) {
        IosStoreKitIntegration.finishPurchase(purchaseToken)
    }

    actual fun obfuscatedAccountId(userId: String): String {
        return userId.hashCode().toUInt().toString(16)
    }
}
