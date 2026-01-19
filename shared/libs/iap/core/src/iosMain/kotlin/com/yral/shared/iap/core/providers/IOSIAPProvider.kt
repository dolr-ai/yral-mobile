package com.yral.shared.iap.core.providers

import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import platform.StoreKit.SKPaymentQueue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import com.yral.shared.iap.core.model.Purchase as IAPPurchase

private val PURCHASE_TIMEOUT: Duration = 5.minutes

internal class IOSIAPProvider : IAPProvider {
    private val paymentQueue: SKPaymentQueue = SKPaymentQueue.defaultQueue()
    private val productFetcher = ProductFetcher()
    private val purchaseManager = PurchaseManager(paymentQueue)
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
        paymentQueue.addTransactionObserver(transactionObserver)
    }

    override suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        productFetcher
            .fetchProducts(productIds)

    @Suppress("ReturnCount")
    override suspend fun purchaseProduct(
        productId: ProductId,
        context: Any?,
        obfuscatedAccountId: String?,
        acknowledgePurchase: Boolean,
    ): Result<IAPPurchase> {
        return try {
            val productIdString = productId.productId
            val skProduct =
                productFetcher.getOrFetchSKProduct(productId)
                    ?: return Result.failure(IAPError.ProductNotFound(productIdString))
            val deferred = purchaseManager.initiatePurchase(skProduct, productIdString)
            try {
                withTimeout(PURCHASE_TIMEOUT) {
                    deferred.await()
                }
            } catch (e: TimeoutCancellationException) {
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
    override suspend fun restorePurchases(acknowledgePurchase: Boolean): Result<List<IAPPurchase>> {
        return try {
            val (existingContinuation, newDeferred) = purchaseManager.startRestore()

            val result =
                if (existingContinuation != null) {
                    try {
                        withTimeout(PURCHASE_TIMEOUT) {
                            existingContinuation.await()
                        }
                    } catch (e: TimeoutCancellationException) {
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
                        purchaseManager.cleanupRestore()
                        throw e
                    }
                } else {
                    paymentQueue.restoreCompletedTransactions()
                    val deferred =
                        newDeferred ?: return Result.failure(
                            IAPError.UnknownError(Exception("Failed to create restore continuation")),
                        )
                    try {
                        withTimeout(PURCHASE_TIMEOUT) {
                            deferred.await()
                        }
                    } catch (e: TimeoutCancellationException) {
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

    override suspend fun isProductPurchased(productId: ProductId): Result<Boolean> =
        try {
            val productIdString = productId.productId
            restorePurchases().map { purchases ->
                purchases.any { purchase ->
                    purchase.productId == productIdString &&
                        purchase.state == PurchaseState.PURCHASED &&
                        (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }
}
