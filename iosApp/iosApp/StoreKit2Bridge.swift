import Foundation
import StoreKit
import iosSharedUmbrella

final class StoreKit2Bridge: AppleStoreKitBridge {
  private let dailyChatProductId = "daily_chat"

  func purchase(
    productId: String,
    appAccountToken: String,
    completion: @escaping (AppleStoreKitPurchaseResult?, String?) -> Void
  ) {
    Task {
      do {
        guard let uuid = UUID(uuidString: appAccountToken) else {
          completion(nil, "Invalid app account token")
          return
        }
        guard let product = try await Product.products(for: [productId]).first else {
          completion(nil, "Product not found: \(productId)")
          return
        }

        let purchaseResult = try await product.purchase(options: [.appAccountToken(uuid)])
        switch purchaseResult {
        case .success(let verificationResult):
          let transaction = try checkVerified(verificationResult)
          completion(toBridgeResult(transaction), nil)
        case .userCancelled:
          completion(nil, "Purchase cancelled")
        case .pending:
          completion(nil, "Purchase is pending approval")
        @unknown default:
          completion(nil, "Unknown purchase result")
        }
      } catch {
        completion(nil, error.localizedDescription)
      }
    }
  }

  func unfinishedPurchases(
    completion: @escaping ([AppleStoreKitPurchaseResult]?, String?) -> Void
  ) {
    Task {
      do {
        var purchases: [AppleStoreKitPurchaseResult] = []
        for await verificationResult in Transaction.unfinished {
          let transaction = try checkVerified(verificationResult)
          if transaction.productID == dailyChatProductId {
            purchases.append(toBridgeResult(transaction))
          }
        }
        completion(purchases, nil)
      } catch {
        completion(nil, error.localizedDescription)
      }
    }
  }

  func finish(
    transactionId: String,
    completion: @escaping (String?) -> Void
  ) {
    Task {
      do {
        for await verificationResult in Transaction.unfinished {
          let transaction = try checkVerified(verificationResult)
          if String(transaction.id) == transactionId {
            await transaction.finish()
            completion(nil)
            return
          }
        }
        completion(nil)
      } catch {
        completion(error.localizedDescription)
      }
    }
  }

  private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
    switch result {
    case .unverified(_, let error):
      throw error
    case .verified(let safe):
      return safe
    }
  }

  private func toBridgeResult(_ transaction: Transaction) -> AppleStoreKitPurchaseResult {
    AppleStoreKitPurchaseResult(
      productId: transaction.productID,
      transactionId: String(transaction.id),
      purchaseTime: Int64(transaction.purchaseDate.timeIntervalSince1970 * 1000)
    )
  }
}
