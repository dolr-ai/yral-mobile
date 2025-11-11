package com.yral.shared.iap.providers

import platform.Foundation.NSError
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionObserverProtocol
import platform.darwin.NSObject

/**
 * Transaction observer that handles SKPaymentTransaction updates.
 * Separate class to avoid mixing Kotlin interfaces with Objective-C protocols.
 *
 * This observer is registered with SKPaymentQueue and receives callbacks for:
 * - Transaction state updates (purchased, failed, restored, deferred)
 * - Restore completion (success or failure)
 */
internal class TransactionObserver(
    private val onTransactionUpdate: (SKPaymentTransaction, String) -> Unit,
    private val onRestoreFinished: () -> Unit,
    private val onRestoreFailed: (NSError) -> Unit,
) : NSObject(),
    SKPaymentTransactionObserverProtocol {
    override fun paymentQueue(
        queue: SKPaymentQueue,
        updatedTransactions: List<*>,
    ) {
        for (transactionObj in updatedTransactions) {
            val transaction = transactionObj as? SKPaymentTransaction ?: continue
            val productId = transaction.payment.productIdentifier
            onTransactionUpdate(transaction, productId)
        }
    }

    override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        onRestoreFinished()
    }

    override fun paymentQueue(
        queue: SKPaymentQueue,
        restoreCompletedTransactionsFailedWithError: NSError,
    ) {
        onRestoreFailed(restoreCompletedTransactionsFailedWithError)
    }
}
