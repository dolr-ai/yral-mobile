package com.yral.shared.iap.core.providers

import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.iap.core.model.SubscriptionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import platform.StoreKit.SKPaymentQueue
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import com.yral.shared.iap.core.model.Purchase as IAPPurchase

private val PURCHASE_TIMEOUT: Duration = 5.minutes

private suspend fun <T> awaitWithRestoreTimeout(
    deferred: Deferred<Result<T>>,
    purchaseManager: PurchaseManager,
): Result<T> =
    try {
        withTimeout(PURCHASE_TIMEOUT) {
            deferred.await()
        }
    } catch (e: TimeoutCancellationException) {
        purchaseManager.cleanupRestore()
        Result.failure(
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

internal class IOSIAPProvider(
    private val appleStoreKitBridge: AppleStoreKitBridge?,
) : IAPProvider {
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
        appAccountToken: String?,
        acknowledgePurchase: Boolean,
    ): Result<IAPPurchase> {
        return try {
            val productIdString = productId.productId
            if (productId == ProductId.DAILY_CHAT && appAccountToken != null && appleStoreKitBridge != null) {
                return purchaseDailyChatWithStoreKit2(
                    productId = productIdString,
                    appAccountToken = appAccountToken,
                )
            }
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
            val unfinishedDailyChatPurchases = restoreUnfinishedStoreKit2Purchases()
            if (unfinishedDailyChatPurchases.isSuccess && !unfinishedDailyChatPurchases.getOrNull().isNullOrEmpty()) {
                return unfinishedDailyChatPurchases
            }

            val (existingContinuation, newDeferred) = purchaseManager.startRestore()

            val result =
                if (existingContinuation != null) {
                    awaitWithRestoreTimeout(existingContinuation, purchaseManager)
                } else {
                    paymentQueue.restoreCompletedTransactions()
                    val deferred =
                        newDeferred ?: return Result.failure(
                            IAPError.UnknownError(Exception("Failed to create restore continuation")),
                        )
                    awaitWithRestoreTimeout(deferred, purchaseManager)
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
        restorePurchases().map { purchases ->
            purchases.any { purchase ->
                purchase.productId == productId &&
                    purchase.state == PurchaseState.PURCHASED &&
                    (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
            }
        }

    override suspend fun consumePurchase(purchaseToken: String): Result<Unit> =
        appleStoreKitBridge?.let { bridge ->
            suspendCancellableCoroutine { continuation ->
                bridge.finish(purchaseToken) { error ->
                    if (error == null) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(Result.failure(IAPError.PurchaseFailed(purchaseToken, Exception(error))))
                    }
                }
            }
        } ?: Result.success(Unit)

    private suspend fun purchaseDailyChatWithStoreKit2(
        productId: String,
        appAccountToken: String,
    ): Result<IAPPurchase> =
        suspendCancellableCoroutine { continuation ->
            appleStoreKitBridge?.purchase(productId, appAccountToken) { result, error ->
                when {
                    result != null -> continuation.resume(Result.success(result.toPurchase()))
                    error != null ->
                        continuation.resume(
                            Result.failure(IAPError.PurchaseFailed(productId, Exception(error))),
                        )
                    else ->
                        continuation.resume(
                            Result.failure(IAPError.PurchaseFailed(productId, Exception("Purchase failed"))),
                        )
                }
            } ?: continuation.resume(
                Result.failure(IAPError.PurchaseFailed(productId, Exception("StoreKit bridge unavailable"))),
            )
        }

    private suspend fun restoreUnfinishedStoreKit2Purchases(): Result<List<IAPPurchase>> {
        val bridge = appleStoreKitBridge ?: return Result.success(emptyList())
        return suspendCancellableCoroutine { continuation ->
            bridge.unfinishedPurchases { results, error ->
                when {
                    results != null -> continuation.resume(Result.success(results.map { it.toPurchase() }))
                    error != null -> continuation.resume(Result.failure(IAPError.NetworkError(Exception(error))))
                    else -> continuation.resume(Result.success(emptyList()))
                }
            }
        }
    }

    private fun AppleStoreKitPurchaseResult.toPurchase(): IAPPurchase =
        IAPPurchase(
            productId = ProductId.fromString(productId),
            purchaseToken = transactionId,
            purchaseTime = purchaseTime,
            state = PurchaseState.PURCHASED,
            expirationDate = null,
            isAutoRenewing = null,
            subscriptionStatus = SubscriptionStatus.UNKNOWN,
            accountIdentifier = null,
        )
}
