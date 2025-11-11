package com.yral.shared.iap.providers

import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.PurchaseState
import platform.StoreKit.SKPaymentQueue
import com.yral.shared.iap.model.Purchase as IAPPurchase

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

            // Initiate purchase and wait for result
            val deferred = purchaseManager.initiatePurchase(skProduct, productIdString)
            deferred.await()
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }
    }

    override suspend fun restorePurchases(): Result<List<IAPPurchase>> {
        return try {
            val (existingContinuation, newDeferred) = purchaseManager.startRestore()

            // If we got an existing continuation, await it
            val result =
                if (existingContinuation != null) {
                    existingContinuation.await()
                } else {
                    // Start restore
                    paymentQueue.restoreCompletedTransactions()

                    // Wait for restore to complete (observer will complete this)
                    val deferred =
                        newDeferred ?: return Result.failure(
                            IAPError.UnknownError(Exception("Failed to create restore continuation")),
                        )
                    deferred.await()
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
