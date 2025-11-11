package com.yral.shared.iap.providers

import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.PurchaseState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import platform.StoreKit.SKPaymentQueue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import com.yral.shared.iap.model.Purchase as IAPPurchase

/**
 * Timeout for purchase operations (5 minutes).
 * This allows sufficient time for user interaction with the payment flow.
 */
private val PURCHASE_TIMEOUT: Duration = 5.minutes

/**
 * iOS implementation of IAPProvider using StoreKit.
 * Orchestrates product fetching, purchases, and purchase restoration for iOS devices.
 *
 * Internal implementation - consumers should use [com.yral.shared.iap.IAPManager] instead.
 */
internal class IOSIAPProvider : IAPProvider {
    private val paymentQueue: SKPaymentQueue = SKPaymentQueue.defaultQueue()
    private val productFetcher = ProductFetcher()
    private val purchaseManager = PurchaseManager(paymentQueue)

    // Create transaction observer
    private val transactionObserver: TransactionObserver =
        TransactionObserver(
            onTransactionUpdate = { transaction, productId ->
                purchaseManager.handleTransactionUpdate(transaction, productId)
            },
            onRestoreFinished = {
                purchaseManager.completeRestore()
            },
            onRestoreFailed = { error ->
                purchaseManager.completeRestoreWithError(error)
            },
        )

    init {
        // Add observer to payment queue
        paymentQueue.addTransactionObserver(transactionObserver)
    }

    override suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        productFetcher
            .fetchProducts(productIds)

    @Suppress("ReturnCount")
    override suspend fun purchaseProduct(
        productId: ProductId,
        context: Any?,
    ): Result<IAPPurchase> {
        return try {
            val productIdString = productId.productId

            // Get SKProduct from cache or fetch it
            val skProduct =
                productFetcher.getOrFetchSKProduct(productId)
                    ?: return Result.failure(IAPError.ProductNotFound(productIdString))

            // Initiate purchase and wait for result with timeout
            val deferred = purchaseManager.initiatePurchase(skProduct, productIdString)
            try {
                withTimeout(PURCHASE_TIMEOUT) {
                    deferred.await()
                }
            } catch (e: TimeoutCancellationException) {
                // Cleanup on timeout - exception is intentionally handled and converted to Result
                purchaseManager.cleanupPendingPurchase(productIdString)
                return Result.failure(
                    IAPError.PurchaseFailed(
                        productIdString,
                        Exception(
                            "Purchase operation timed out after ${PURCHASE_TIMEOUT.inWholeSeconds} seconds",
                            e,
                        ),
                    ),
                )
            } catch (e: CancellationException) {
                // Cleanup on cancellation - re-throw to respect coroutine cancellation
                purchaseManager.cleanupPendingPurchase(productIdString)
                throw e
            }
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }
    }

    @Suppress("ReturnCount")
    override suspend fun restorePurchases(): Result<List<IAPPurchase>> {
        return try {
            val (existingContinuation, newDeferred) = purchaseManager.startRestore()

            // If we got an existing continuation, await it with timeout
            val result =
                if (existingContinuation != null) {
                    try {
                        withTimeout(PURCHASE_TIMEOUT) {
                            existingContinuation.await()
                        }
                    } catch (e: TimeoutCancellationException) {
                        // Cleanup on timeout - exception is intentionally handled and converted to Result
                        purchaseManager.cleanupRestore()
                        return Result.failure(
                            IAPError.NetworkError(
                                Exception(
                                    "Restore operation timed out after ${PURCHASE_TIMEOUT.inWholeSeconds} seconds",
                                    e,
                                ),
                            ),
                        )
                    } catch (e: CancellationException) {
                        // Cleanup on cancellation - re-throw to respect coroutine cancellation
                        purchaseManager.cleanupRestore()
                        throw e
                    }
                } else {
                    // Start restore
                    paymentQueue.restoreCompletedTransactions()

                    // Wait for restore to complete (observer will complete this) with timeout
                    val deferred =
                        newDeferred ?: return Result.failure(
                            IAPError.UnknownError(Exception("Failed to create restore continuation")),
                        )
                    try {
                        withTimeout(PURCHASE_TIMEOUT) {
                            deferred.await()
                        }
                    } catch (e: TimeoutCancellationException) {
                        // Cleanup on timeout - exception is intentionally handled and converted to Result
                        purchaseManager.cleanupRestore()
                        return Result.failure(
                            IAPError.NetworkError(
                                Exception(
                                    "Restore operation timed out after ${PURCHASE_TIMEOUT.inWholeSeconds} seconds",
                                    e,
                                ),
                            ),
                        )
                    } catch (e: CancellationException) {
                        // Cleanup on cancellation - re-throw to respect coroutine cancellation
                        purchaseManager.cleanupRestore()
                        throw e
                    }
                }
            result
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }
    }

    override suspend fun isProductPurchased(productId: ProductId): Boolean =
        runCatching {
            val productIdString = productId.productId
            val restoreResult = restorePurchases()
            restoreResult
                .getOrNull()
                ?.any { it.productId == productIdString && it.state == PurchaseState.PURCHASED }
                ?: false
        }.getOrDefault(false)
}
