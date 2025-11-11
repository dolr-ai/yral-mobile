package com.yral.shared.iap.providers

import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.PurchaseState
import com.yral.shared.iap.util.withLock
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSLock
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.dataWithContentsOfURL
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransaction
import com.yral.shared.iap.model.Purchase as IAPPurchase

/**
 * Manages purchase flow, transaction handling, and purchase restoration for iOS StoreKit.
 * Handles pending purchases, transaction state management, and receipt extraction.
 */
internal class PurchaseManager(
    private val paymentQueue: SKPaymentQueue,
) {
    companion object {
        // SKError codes (these are not available in Kotlin/Native, so we define them)
        private const val SK_ERROR_PAYMENT_CANCELLED = 2L
        private const val SK_ERROR_PAYMENT_NOT_ALLOWED = 3L
        private const val SK_ERROR_STORE_PRODUCT_NOT_AVAILABLE = 5L

        // SKPaymentTransactionState enum values
        private const val TRANSACTION_STATE_PURCHASING = 0L
        private const val TRANSACTION_STATE_PURCHASED = 1L
        private const val TRANSACTION_STATE_FAILED = 2L
        private const val TRANSACTION_STATE_RESTORED = 3L
        private const val TRANSACTION_STATE_DEFERRED = 4L
    }

    // Track pending purchases by product identifier
    private val pendingPurchases = mutableMapOf<String, CompletableDeferred<Result<IAPPurchase>>>()
    private val pendingPurchasesLock = NSLock()

    // Track restore operation
    private var restoreContinuation: CompletableDeferred<Result<List<IAPPurchase>>>? = null
    private val restoredPurchases = mutableListOf<IAPPurchase>()
    private val restoreLock = NSLock()

    /**
     * Initiates a purchase for the given SKProduct.
     *
     * @param skProduct SKProduct to purchase
     * @param productIdString Product ID string
     * @return CompletableDeferred that will be completed when transaction finishes
     */
    fun initiatePurchase(
        skProduct: platform.StoreKit.SKProduct,
        productIdString: String,
    ): CompletableDeferred<Result<IAPPurchase>> {
        // Create payment and add to queue
        val payment = SKPayment.paymentWithProduct(skProduct)
        val deferred = CompletableDeferred<Result<IAPPurchase>>()

        // Store deferred for this transaction
        // Note: We use productId as key since transaction ID isn't available yet
        // We'll match by product identifier in the observer
        pendingPurchasesLock.withLock {
            pendingPurchases[productIdString] = deferred
        }

        paymentQueue.addPayment(payment)
        return deferred
    }

    /**
     * Starts restore purchases operation.
     *
     * @return Pair of (existing continuation if restore in progress, new continuation otherwise)
     */
    fun startRestore(): Pair<
        CompletableDeferred<Result<List<IAPPurchase>>>?,
        CompletableDeferred<Result<List<IAPPurchase>>>?,
    > =
        restoreLock.withLock {
            if (restoreContinuation != null) {
                // Restore already in progress, return existing continuation
                Pair(restoreContinuation, null)
            } else {
                // Start new restore operation
                restoredPurchases.clear()
                val deferred = CompletableDeferred<Result<List<IAPPurchase>>>()
                restoreContinuation = deferred
                Pair(null, deferred)
            }
        }

    /**
     * Handles transaction state updates from the observer.
     *
     * @param transaction SKPaymentTransaction
     * @param productId Product ID string
     */
    fun handleTransactionUpdate(
        transaction: SKPaymentTransaction,
        productId: String,
    ) {
        val state = transaction.transactionState
        when (state.value) {
            TRANSACTION_STATE_PURCHASING -> {
                // SKPaymentTransactionState.Purchasing
                // Transaction is being processed, do nothing
            }
            TRANSACTION_STATE_PURCHASED -> {
                // SKPaymentTransactionState.Purchased
                handlePurchasedTransaction(transaction, productId)
            }
            TRANSACTION_STATE_FAILED -> {
                // SKPaymentTransactionState.Failed
                handleFailedTransaction(transaction, productId)
            }
            TRANSACTION_STATE_RESTORED -> {
                // SKPaymentTransactionState.Restored
                handleRestoredTransaction(transaction, productId)
            }
            TRANSACTION_STATE_DEFERRED -> {
                // SKPaymentTransactionState.Deferred
                handleDeferredTransaction(transaction, productId)
            }
            else -> {
                // Unknown state
                handleFailedTransaction(
                    transaction,
                    productId,
                    Exception("Unknown transaction state: $state"),
                )
            }
        }
    }

    /**
     * Completes restore operation with success.
     */
    fun completeRestore() {
        restoreLock.withLock {
            val continuation = restoreContinuation
            if (continuation != null) {
                restoreContinuation = null
                continuation.complete(Result.success(restoredPurchases.toList()))
            }
        }
    }

    /**
     * Completes restore operation with error.
     */
    fun completeRestoreWithError(error: NSError) {
        restoreLock.withLock {
            val continuation = restoreContinuation
            if (continuation != null) {
                restoreContinuation = null
                continuation.complete(
                    Result.failure(
                        IAPError.NetworkError(
                            Exception("Failed to restore purchases: ${error.localizedDescription}"),
                        ),
                    ),
                )
            }
        }
    }

    // MARK: - Private Transaction Handlers

    /**
     * Handles a successfully purchased transaction.
     */
    private fun handlePurchasedTransaction(
        transaction: SKPaymentTransaction,
        productId: String,
    ) {
        val purchase = convertTransactionToPurchase(transaction, PurchaseState.PURCHASED)
        val receipt = getReceiptData()

        val purchaseWithReceipt = purchase.copy(receipt = receipt)

        // Complete pending purchase
        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.complete(Result.success(purchaseWithReceipt))
        }

        // Finish the transaction
        paymentQueue.finishTransaction(transaction)
    }

    /**
     * Handles a failed transaction.
     */
    private fun handleFailedTransaction(
        transaction: SKPaymentTransaction,
        productId: String,
        customError: Exception? = null,
    ) {
        val error =
            customError
                ?: transaction.error?.let {
                    when (it.code.toLong()) {
                        SK_ERROR_PAYMENT_CANCELLED -> IAPError.PurchaseCancelled(productId)
                        SK_ERROR_PAYMENT_NOT_ALLOWED ->
                            IAPError.PurchaseFailed(
                                productId,
                                Exception("Payment not allowed: ${it.localizedDescription}"),
                            )
                        SK_ERROR_STORE_PRODUCT_NOT_AVAILABLE -> IAPError.ProductNotFound(productId)
                        else ->
                            IAPError.PurchaseFailed(
                                productId,
                                Exception("Transaction failed: ${it.localizedDescription}"),
                            )
                    }
                } ?: IAPError.PurchaseFailed(productId, Exception("Transaction failed"))

        // Complete pending purchase with error
        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.complete(Result.failure(error))
        }

        // Finish the transaction
        paymentQueue.finishTransaction(transaction)
    }

    /**
     * Handles a restored transaction.
     */
    private fun handleRestoredTransaction(
        transaction: SKPaymentTransaction,
        productId: String,
    ) {
        val purchase = convertTransactionToPurchase(transaction, PurchaseState.PURCHASED)
        val receipt = getReceiptData()

        val purchaseWithReceipt = purchase.copy(receipt = receipt)

        // Add to restored purchases list
        restoreLock.withLock {
            restoredPurchases.add(purchaseWithReceipt)
        }

        // Also complete pending purchase if any (for restore operations)
        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.complete(Result.success(purchaseWithReceipt))
        }

        // Finish the transaction
        paymentQueue.finishTransaction(transaction)
    }

    /**
     * Handles a deferred transaction (waiting for approval, e.g., Ask to Buy).
     */
    @Suppress("UNUSED_PARAMETER")
    private fun handleDeferredTransaction(
        transaction: SKPaymentTransaction,
        productId: String,
    ) {
        // Transaction is deferred (e.g., waiting for parental approval)
        // Don't finish the transaction, but notify the caller
        val error =
            IAPError.PurchaseFailed(
                productId,
                Exception("Purchase is pending approval (Ask to Buy)"),
            )

        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.complete(Result.failure(error))
        }
        // Don't finish transaction - it will be updated when approved/denied
    }

    /**
     * Converts SKPaymentTransaction to IAPPurchase model.
     */
    private fun convertTransactionToPurchase(
        transaction: SKPaymentTransaction,
        state: PurchaseState,
    ): IAPPurchase {
        val productId = transaction.payment.productIdentifier
        val transactionId = transaction.transactionIdentifier ?: ""
        // Convert NSDate to milliseconds timestamp
        // Note: Transaction date is optional and NSDate properties are not easily accessible in Kotlin/Native
        // Using 0 as fallback - in production, parse receipt for accurate timestamps
        val purchaseTime = 0L // Transaction date parsing would require additional interop setup

        return IAPPurchase(
            productId = productId,
            // Use transaction ID as token
            purchaseToken = transactionId,
            purchaseTime = purchaseTime,
            state = state,
        )
    }

    /**
     * Gets receipt data as base64 string.
     */
    @Suppress("ReturnCount")
    private fun getReceiptData(): String? {
        val receiptURL = NSBundle.mainBundle.appStoreReceiptURL ?: return null
        val receiptData = NSData.dataWithContentsOfURL(receiptURL) ?: return null
        return receiptData.base64EncodedStringWithOptions(0u)
    }
}
