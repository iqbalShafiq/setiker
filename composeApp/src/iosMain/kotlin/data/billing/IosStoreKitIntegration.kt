package data.billing

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Swift StoreKit 2 registers handlers here on iOS app launch.
 * See iosApp/StoreKit2Manager.swift
 */
object IosStoreKitIntegration {
  private var queryProductsHandler:
      ((List<String>, (List<Map<String, String?>>) -> Unit) -> Unit)? = null
  private var purchaseHandler:
      ((String, (Map<String, String?>?) -> Unit) -> Unit)? = null
  private var queryPendingPurchasesHandler:
      (((List<Map<String, String?>>) -> Unit) -> Unit)? = null
  private var finishPurchaseHandler: ((String, (Boolean) -> Unit) -> Unit)? = null

  fun registerStoreKitHandlers(
      queryProducts: (List<String>, (List<Map<String, String?>>) -> Unit) -> Unit,
      purchase: (String, (Map<String, String?>?) -> Unit) -> Unit,
      queryPendingPurchases: ((List<Map<String, String?>>) -> Unit) -> Unit,
      finishPurchase: (String, (Boolean) -> Unit) -> Unit,
  ) {
    queryProductsHandler = queryProducts
    purchaseHandler = purchase
    queryPendingPurchasesHandler = queryPendingPurchases
    finishPurchaseHandler = finishPurchase
  }

  internal suspend fun queryProducts(productIds: List<String>): List<PlatformStoreProduct> {
    val handler = queryProductsHandler ?: return emptyList()
    return suspendCancellableCoroutine { continuation ->
      handler(productIds) { results ->
        continuation.resume(
            results.map { row ->
              PlatformStoreProduct(
                  productId = row["productId"].orEmpty(),
                  formattedPrice = row["formattedPrice"],
              )
            })
      }
    }
  }

  internal suspend fun purchase(productId: String): PlatformPurchase? {
    val handler = purchaseHandler ?: return null
    return suspendCancellableCoroutine { continuation ->
      handler(productId) { payload ->
        if (payload == null) {
          continuation.resume(null)
        } else {
          continuation.resume(
              PlatformPurchase(
                  productId = payload["productId"].orEmpty(),
                  purchaseToken = payload["purchaseToken"].orEmpty(),
                  transactionId = payload["transactionId"],
                  originalTransactionId = payload["originalTransactionId"],
              ))
        }
      }
    }
  }

  internal suspend fun queryPendingPurchases(): List<PlatformPurchase> {
    val handler = queryPendingPurchasesHandler ?: return emptyList()
    return suspendCancellableCoroutine { continuation ->
      handler { results ->
        continuation.resume(
            results.map { row ->
              PlatformPurchase(
                  productId = row["productId"].orEmpty(),
                  purchaseToken = row["purchaseToken"].orEmpty(),
                  transactionId = row["transactionId"],
                  originalTransactionId = row["originalTransactionId"],
              )
            })
      }
    }
  }

  internal suspend fun finishPurchase(transactionId: String): Boolean {
    val handler = finishPurchaseHandler ?: return false
    return suspendCancellableCoroutine { continuation ->
      handler(transactionId) { success -> continuation.resume(success) }
    }
  }
}
