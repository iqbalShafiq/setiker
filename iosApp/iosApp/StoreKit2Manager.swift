import Foundation
import StoreKit
import ComposeApp

@available(iOS 15.0, *)
final class StoreKit2Manager {
    static let shared = StoreKit2Manager()

    private init() {}

    func registerHandlers() {
        IosStoreKitIntegrationKt.registerStoreKitHandlers(
            queryProducts: { ids, completion in
                Task {
                    do {
                        let products = try await Product.products(for: Set(ids))
                        let mapped: [[String: String]] = products.map { product in
                            [
                                "productId": product.id,
                                "formattedPrice": product.displayPrice
                            ]
                        }
                        completion(mapped)
                    } catch {
                        completion([])
                    }
                }
            },
            purchase: { productId, completion in
                Task {
                    do {
                        let products = try await Product.products(for: [productId])
                        guard let product = products.first else {
                            completion(nil)
                            return
                        }
                        let result = try await product.purchase()
                        switch result {
                        case .success(let verification):
                            switch verification {
                            case .verified(let transaction):
                                completion([
                                    "productId": productId,
                                    "purchaseToken": verification.jwsRepresentation,
                                    "transactionId": String(transaction.id),
                                    "originalTransactionId": String(transaction.originalID)
                                ])
                            case .unverified:
                                completion(nil)
                            }
                        case .userCancelled, .pending:
                            completion(nil)
                        @unknown default:
                            completion(nil)
                        }
                    } catch {
                        completion(nil)
                    }
                }
            },
            queryPendingPurchases: { completion in
                Task {
                    var purchases: [[String: String]] = []
                    var seen = Set<String>()

                    for await result in Transaction.currentEntitlements {
                        if let payload = Self.payload(from: result), seen.insert(payload["transactionId"] ?? "").inserted {
                            purchases.append(payload)
                        }
                    }
                    for await result in Transaction.unfinished {
                        if let payload = Self.payload(from: result), seen.insert(payload["transactionId"] ?? "").inserted {
                            purchases.append(payload)
                        }
                    }
                    completion(purchases)
                }
            },
            finishPurchase: { transactionId, completion in
                Task {
                    guard let id = UInt64(transactionId) else {
                        completion(false)
                        return
                    }
                    if let latest = await Transaction.latest(for: id) {
                        switch latest {
                        case .verified(let transaction):
                            await transaction.finish()
                            completion(true)
                            return
                        case .unverified:
                            break
                        }
                    }
                    completion(false)
                }
            }
        )
    }

    private static func payload(from result: VerificationResult<Transaction>) -> [String: String]? {
        switch result {
        case .verified(let transaction):
            return [
                "productId": transaction.productID,
                "purchaseToken": result.jwsRepresentation,
                "transactionId": String(transaction.id),
                "originalTransactionId": String(transaction.originalID)
            ]
        case .unverified:
            return nil
        }
    }
}
