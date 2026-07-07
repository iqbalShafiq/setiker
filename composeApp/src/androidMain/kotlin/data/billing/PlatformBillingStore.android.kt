package data.billing

import android.app.Activity
import android.util.Base64
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.resume

actual class PlatformBillingStore(
    private val activityProvider: () -> Activity?
) {
    private val purchaseDeferred = CompletableDeferred<PlatformPurchase?>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases.isNullOrEmpty()) {
            if (!purchaseDeferred.isCompleted) {
                purchaseDeferred.complete(null)
            }
            return@PurchasesUpdatedListener
        }
        val purchase = purchases.first()
        if (!purchaseDeferred.isCompleted) {
            purchaseDeferred.complete(
                PlatformPurchase(
                    productId = purchase.products.firstOrNull().orEmpty(),
                    purchaseToken = purchase.purchaseToken
                )
            )
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(activityProvider() ?: throw IllegalStateException("Activity required"))
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    actual suspend fun queryProducts(productIds: List<String>): List<PlatformStoreProduct> {
        if (productIds.isEmpty()) return emptyList()
        connect()
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            ).build()
        val inAppResult = billingClient.queryProductDetails(inAppParams)
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            ).build()
        val subsResult = billingClient.queryProductDetails(subsParams)

        val products = buildList {
            inAppResult.productDetailsList.orEmpty().forEach { add(it) }
            subsResult.productDetailsList.orEmpty().forEach { add(it) }
        }
        return products.map { details ->
            PlatformStoreProduct(
                productId = details.productId,
                formattedPrice = details.oneTimePurchaseOfferDetails?.formattedPrice
                    ?: details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            )
        }
    }

    actual suspend fun launchPurchase(productId: String, obfuscatedAccountId: String): PlatformPurchase? {
        connect()
        val details = queryProductDetails(productId) ?: return null
        val productType = if (details.productType == BillingClient.ProductType.SUBS) {
            BillingClient.ProductType.SUBS
        } else {
            BillingClient.ProductType.INAPP
        }
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { if (offerToken != null) setOfferToken(offerToken) }
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setObfuscatedAccountId(obfuscatedAccountId)
            .build()
        val activity = activityProvider() ?: return null
        val deferred = CompletableDeferred<PlatformPurchase?>()
        val listener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
                val purchase = purchases.first()
                deferred.complete(
                    PlatformPurchase(
                        productId = purchase.products.firstOrNull().orEmpty(),
                        purchaseToken = purchase.purchaseToken
                    )
                )
            } else {
                deferred.complete(null)
            }
        }
        // Rebuild client with one-shot listener for this purchase
        val client = BillingClient.newBuilder(activity)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        connectClient(client)
        val launchResult = client.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return null
        }
        return deferred.await()
    }

    actual suspend fun queryPendingPurchases(): List<PlatformPurchase> {
        connect()
        val inApp = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ).purchasesList
        val subs = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ).purchasesList
        return (inApp + subs).map {
            PlatformPurchase(
                productId = it.products.firstOrNull().orEmpty(),
                purchaseToken = it.purchaseToken
            )
        }
    }

    actual suspend fun consumePurchase(purchaseToken: String) {
        connect()
        billingClient.consumePurchase(
            ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
        )
    }

    actual suspend fun acknowledgePurchase(purchaseToken: String) {
        connect()
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
        )
    }

    actual fun obfuscatedAccountId(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private suspend fun queryProductDetails(productId: String): ProductDetails? {
        val inApp = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                ).build()
        ).productDetailsList.orEmpty()
        if (inApp.isNotEmpty()) return inApp.first()
        return billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                ).build()
        ).productDetailsList?.firstOrNull()
    }

    private suspend fun connect() = connectClient(billingClient)

    private suspend fun connectClient(client: BillingClient) = withContext(Dispatchers.Main) {
        if (client.isReady) return@withContext
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
        }
    }
}
