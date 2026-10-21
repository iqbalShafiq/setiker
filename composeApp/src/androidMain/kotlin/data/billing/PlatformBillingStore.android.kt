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
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

actual class PlatformBillingStore(
    private val activityProvider: () -> Activity?
) {
    /**
     * Completed by [purchasesUpdatedListener] for the purchase currently started via
     * [launchPurchase]. Only touched on the main thread (the billing callbacks and our launch
     * path both run there), so a plain field is enough.
     */
    private var activePurchase: CompletableDeferred<PlatformPurchase?>? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val deferred = activePurchase
        activePurchase = null
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            val purchase = purchases.first()
            deferred?.complete(
                PlatformPurchase(
                    productId = purchase.products.firstOrNull().orEmpty(),
                    purchaseToken = purchase.purchaseToken
                )
            )
        } else {
            deferred?.complete(null)
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(activityProvider() ?: throw IllegalStateException("Activity required"))
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // PBL 8+: the library re-establishes transient disconnections on its own; there is no
        // need to recreate the client or retry manually from onBillingServiceDisconnected.
        .enableAutoServiceReconnection()
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

    actual suspend fun launchPurchase(productId: String, obfuscatedAccountId: String): PlatformPurchase? =
        withContext(Dispatchers.Main) {
            connect()
            val details = queryProductDetails(productId) ?: return@withContext null
            val productType = if (details.productType == BillingClient.ProductType.SUBS) {
                BillingClient.ProductType.SUBS
            } else {
                BillingClient.ProductType.INAPP
            }
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (productType == BillingClient.ProductType.SUBS && offerToken == null) {
                // PBL 8+ requires an offer token for subscription purchases; without one the
                // billing flow cannot be started.
                return@withContext null
            }
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .apply { if (offerToken != null) setOfferToken(offerToken) }
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .setObfuscatedAccountId(obfuscatedAccountId)
                .build()
            val activity = activityProvider() ?: return@withContext null

            // Reuse the single client (and its listener) instead of building a throwaway client
            // per purchase; the listener routes the result to the in-flight deferred.
            val deferred = CompletableDeferred<PlatformPurchase?>()
            activePurchase = deferred
            val launchResult = billingClient.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                activePurchase = null
                return@withContext null
            }
            deferred.await()
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
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    // Auto-reconnection is enabled, so a later API call re-establishes the
                    // connection. Resuming here keeps this continuation from hanging forever.
                    if (cont.isActive) cont.resume(Unit)
                }
            })
        }
    }
}
