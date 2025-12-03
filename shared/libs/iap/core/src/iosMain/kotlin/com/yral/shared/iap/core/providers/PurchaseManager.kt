package com.yral.shared.iap.core.providers

import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.iap.core.model.SubscriptionStatus
import com.yral.shared.iap.core.util.withLock
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSLock
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransaction
import kotlin.time.Duration.Companion.seconds
import com.yral.shared.iap.core.model.Purchase as IAPPurchase

internal class PurchaseManager(
    private val paymentQueue: SKPaymentQueue,
) {
    companion object {
        private const val SK_ERROR_PAYMENT_CANCELLED = 2L
        private const val SK_ERROR_PAYMENT_NOT_ALLOWED = 3L
        private const val SK_ERROR_STORE_PRODUCT_NOT_AVAILABLE = 5L
        private const val TRANSACTION_STATE_PURCHASING = 0L
        private const val TRANSACTION_STATE_PURCHASED = 1L
        private const val TRANSACTION_STATE_FAILED = 2L
        private const val TRANSACTION_STATE_RESTORED = 3L
        private const val TRANSACTION_STATE_DEFERRED = 4L
    }

    private val pendingPurchases = mutableMapOf<String, CompletableDeferred<Result<IAPPurchase>>>()
    private val pendingPurchasesLock = NSLock()
    private var restoreContinuation: CompletableDeferred<Result<List<IAPPurchase>>>? = null
    private val restoredPurchases = mutableListOf<IAPPurchase>()
    private val restoreLock = NSLock()

    fun initiatePurchase(
        skProduct: platform.StoreKit.SKProduct,
        productIdString: String,
    ): CompletableDeferred<Result<IAPPurchase>> {
        val payment = SKPayment.paymentWithProduct(skProduct)
        val deferred = CompletableDeferred<Result<IAPPurchase>>()
        pendingPurchasesLock.withLock {
            pendingPurchases[productIdString] = deferred
        }

        paymentQueue.addPayment(payment)
        return deferred
    }

    fun startRestore(): Pair<
        CompletableDeferred<Result<List<IAPPurchase>>>?,
        CompletableDeferred<Result<List<IAPPurchase>>>?,
    > =
        restoreLock.withLock {
            if (restoreContinuation != null) {
                Pair(restoreContinuation, null)
            } else {
                restoredPurchases.clear()
                val deferred = CompletableDeferred<Result<List<IAPPurchase>>>()
                restoreContinuation = deferred
                Pair(null, deferred)
            }
        }

    fun handleTransactionUpdate(
        transaction: SKPaymentTransaction,
        productId: String,
    ) {
        val state = transaction.transactionState
        when (state.value) {
            TRANSACTION_STATE_PURCHASING -> return
            TRANSACTION_STATE_PURCHASED -> handlePurchasedTransaction(transaction, productId)
            TRANSACTION_STATE_FAILED -> handleFailedTransaction(transaction, productId)
            TRANSACTION_STATE_RESTORED -> handleRestoredTransaction(transaction, productId)
            TRANSACTION_STATE_DEFERRED -> handleDeferredTransaction(transaction, productId)
            else ->
                handleFailedTransaction(
                    transaction,
                    productId,
                    Exception("Unknown transaction state: $state"),
                )
        }
    }

    fun completeRestore() {
        restoreLock.withLock {
            val continuation = restoreContinuation
            if (continuation != null && !continuation.isCompleted) {
                restoreContinuation = null
                continuation.complete(Result.success(restoredPurchases.toList()))
            }
        }
    }

    fun completeRestoreWithError(error: NSError) {
        restoreLock.withLock {
            val continuation = restoreContinuation
            if (continuation != null && !continuation.isCompleted) {
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

    private fun handlePurchasedTransaction(
        transaction: SKPaymentTransaction,
        productId: String,
    ) {
        val purchase = convertTransactionToPurchase(transaction, PurchaseState.PURCHASED)
        val receipt = getReceiptData()
        val purchaseWithReceipt = purchase.copy(receipt = receipt)
        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.let { deferred ->
                if (!deferred.isCompleted) {
                    deferred.complete(Result.success(purchaseWithReceipt))
                }
            }
        }
        paymentQueue.finishTransaction(transaction)
    }

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
        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.let { deferred ->
                if (!deferred.isCompleted) {
                    deferred.complete(Result.failure(error))
                }
            }
        }
        paymentQueue.finishTransaction(transaction)
    }

    private fun handleRestoredTransaction(
        transaction: SKPaymentTransaction,
        productId: String,
    ) {
        val purchase = convertTransactionToPurchase(transaction, PurchaseState.PURCHASED)
        val receipt = getReceiptData()
        val purchaseWithReceipt = purchase.copy(receipt = receipt)
        restoreLock.withLock {
            restoredPurchases.add(purchaseWithReceipt)
        }
        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.let { deferred ->
                if (!deferred.isCompleted) {
                    deferred.complete(Result.success(purchaseWithReceipt))
                }
            }
        }
        paymentQueue.finishTransaction(transaction)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleDeferredTransaction(
        transaction: SKPaymentTransaction,
        productId: String,
    ) {
        val error =
            IAPError.PurchaseFailed(
                productId,
                Exception("Purchase is pending approval (Ask to Buy)"),
            )

        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.let { deferred ->
                if (!deferred.isCompleted) {
                    deferred.complete(Result.failure(error))
                }
            }
        }
    }

    private fun convertTransactionToPurchase(
        transaction: SKPaymentTransaction,
        state: PurchaseState,
    ): IAPPurchase {
        val productId = transaction.payment.productIdentifier
        val transactionId = transaction.transactionIdentifier ?: ""
        val purchaseTime =
            transaction.transactionDate
                ?.timeIntervalSince1970
                ?.seconds
                ?.inWholeMilliseconds ?: 0L

        return IAPPurchase(
            productId = productId,
            purchaseToken = transactionId,
            purchaseTime = purchaseTime,
            state = state,
            expirationDate = null,
            isAutoRenewing = null,
            subscriptionStatus = SubscriptionStatus.UNKNOWN,
            accountIdentifier = null,
        )
    }

    @Suppress("ReturnCount")
    private fun getReceiptData(): String? {
        val receiptURL = NSBundle.mainBundle.appStoreReceiptURL ?: return null
        val receiptData = NSData.dataWithContentsOfURL(receiptURL) ?: return null
        return receiptData.base64EncodedStringWithOptions(0u)
    }

    fun cleanupPendingPurchase(productId: String) {
        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.let { deferred ->
                if (!deferred.isCompleted) {
                    deferred.complete(
                        Result.failure(
                            IAPError.PurchaseFailed(
                                productId,
                                Exception("Purchase operation was cancelled or timed out"),
                            ),
                        ),
                    )
                }
            }
        }
    }

    fun cleanupRestore() {
        restoreLock.withLock {
            restoreContinuation?.let { deferred ->
                if (!deferred.isCompleted) {
                    restoreContinuation = null
                    deferred.complete(
                        Result.failure(
                            IAPError.NetworkError(
                                Exception("Restore operation was cancelled or timed out"),
                            ),
                        ),
                    )
                }
            }
        }
    }
}
