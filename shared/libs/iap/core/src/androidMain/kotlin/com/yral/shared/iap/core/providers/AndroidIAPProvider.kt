package com.yral.shared.iap.core.providers

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import com.yral.shared.iap.core.model.Purchase as IAPPurchase

private val PURCHASE_TIMEOUT: Duration = 5.minutes

internal class AndroidIAPProvider(
    context: Context,
    appDispatchers: AppDispatchers,
) : IAPProvider {
    private val pendingPurchases = mutableMapOf<String, CompletableDeferred<Result<IAPPurchase>>>()
    private val pendingPurchasesAcknowledgeFlags = mutableMapOf<String, Boolean>()
    private val pendingPurchasesLock = Mutex()
    private val callbackScope = CoroutineScope(SupervisorJob() + appDispatchers.network)

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            handlePurchaseUpdate(billingResult, purchases)
        }

    private val connectionManager = BillingClientConnectionManager(context, purchasesUpdatedListener)
    private val productFetcher = ProductFetcher(connectionManager)
    private val purchaseManager = PurchaseManager(connectionManager, appDispatchers)

    override suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        productFetcher
            .fetchProducts(productIds)

    @Suppress("ReturnCount", "LongMethod")
    override suspend fun purchaseProduct(
        productId: ProductId,
        context: Any?,
        obfuscatedAccountId: String?,
        acknowledgePurchase: Boolean,
    ): Result<IAPPurchase> {
        return try {
            val productIdString = productId.productId
            val activity = context as? Activity
            val productDetails =
                if (activity == null) {
                    return Result.failure(
                        IAPError.PurchaseFailed(
                            productIdString,
                            Exception(
                                "Activity context is required for purchase. " +
                                    "Pass Activity from LocalContext.current in Compose.",
                            ),
                        ),
                    )
                } else {
                    productFetcher.queryProductDetailsForPurchase(productId)
                        ?: return Result.failure(IAPError.ProductNotFound(productIdString))
                }
            val client = connectionManager.ensureReady()
            val flowParams =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams
                                .newBuilder()
                                .setProductDetails(productDetails)
                                .apply {
                                    productDetails.subscriptionOfferDetails?.firstOrNull()?.let { offer ->
                                        setOfferToken(offer.offerToken)
                                    }
                                }.build(),
                        ),
                    ).apply { obfuscatedAccountId?.let { setObfuscatedAccountId(it) } }
                    .build()

            val deferred = CompletableDeferred<Result<IAPPurchase>>()
            pendingPurchasesLock.withLock {
                pendingPurchases[productIdString] = deferred
                pendingPurchasesAcknowledgeFlags[productIdString] = acknowledgePurchase
            }

            val billingResult = client.launchBillingFlow(activity, flowParams)

            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchasesLock.withLock {
                    pendingPurchases.remove(productIdString)?.let {
                        pendingPurchasesAcknowledgeFlags.remove(productIdString)
                        if (!it.isCompleted) {
                            it.complete(
                                Result.failure(
                                    IAPError.PurchaseFailed(
                                        productIdString,
                                        Exception("Billing flow failed: ${billingResult.debugMessage}"),
                                    ),
                                ),
                            )
                        }
                    }
                }
                return Result.failure(
                    IAPError.PurchaseFailed(
                        productIdString,
                        Exception("Billing flow failed: ${billingResult.debugMessage}"),
                    ),
                )
            }

            withTimeout(PURCHASE_TIMEOUT) {
                deferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            cleanupPendingPurchase(productId.productId)
            Result.failure(
                IAPError.PurchaseFailed(
                    productId.productId,
                    Exception(
                        "Purchase operation timed out after ${PURCHASE_TIMEOUT.inWholeSeconds} seconds",
                        e,
                    ),
                ),
            )
        } catch (e: CancellationException) {
            cleanupPendingPurchase(productId.productId)
            throw e
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }
    }

    override suspend fun restorePurchases(acknowledgePurchase: Boolean): Result<List<IAPPurchase>> =
        purchaseManager
            .restorePurchases(acknowledgePurchase)

    override suspend fun isProductPurchased(productId: ProductId): Result<Boolean> =
        try {
            restorePurchases().map { purchases ->
                purchases.any { purchase ->
                    purchase.productId == productId &&
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

    private fun handlePurchaseUpdate(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            handleBillingError(billingResult, purchases)
            return
        }

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> handleSuccessfulPurchases(purchases)
            BillingClient.BillingResponseCode.USER_CANCELED -> handleCancelledPurchases(purchases)
            else -> handleBillingError(billingResult, purchases)
        }
    }

    private fun handleSuccessfulPurchases(purchases: List<Purchase>?) {
        purchases?.forEach { purchase ->
            val productId = purchase.products.firstOrNull() ?: return@forEach
            val iapPurchase = purchaseManager.convertPurchase(purchase)
            callbackScope.launch {
                val shouldAcknowledge =
                    pendingPurchasesLock.withLock {
                        pendingPurchasesAcknowledgeFlags[productId] ?: true
                    }
                if (shouldAcknowledge) {
                    try {
                        val client = connectionManager.ensureReady()
                        purchaseManager.acknowledgePurchaseIfNeeded(client, purchase)
                    } catch (e: TimeoutCancellationException) {
                        throw e
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // The acknowledgment failure is handled in acknowledgePurchaseIfNeeded callback
                    }
                }
            }
            callbackScope.launch {
                pendingPurchasesLock.withLock {
                    pendingPurchases.remove(productId)?.let { deferred ->
                        pendingPurchasesAcknowledgeFlags.remove(productId)
                        if (!deferred.isCompleted) {
                            deferred.complete(Result.success(iapPurchase))
                        }
                    }
                }
            }
        }
    }

    private fun handleCancelledPurchases(purchases: List<Purchase>?) {
        purchases?.forEach { purchase ->
            val productId = purchase.products.firstOrNull() ?: return@forEach
            val error = IAPError.PurchaseCancelled(productId)
            callbackScope.launch {
                pendingPurchasesLock.withLock {
                    pendingPurchases.remove(productId)?.let { deferred ->
                        pendingPurchasesAcknowledgeFlags.remove(productId)
                        if (!deferred.isCompleted) {
                            deferred.complete(Result.failure(error))
                        }
                    }
                }
            }
        }
    }

    private fun handleBillingError(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        val error =
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.ERROR,
                -> IAPError.BillingUnavailable(Exception(billingResult.debugMessage))
                else -> IAPError.UnknownError(Exception(billingResult.debugMessage))
            }

        purchases?.forEach { purchase ->
            val productId = purchase.products.firstOrNull() ?: return@forEach
            callbackScope.launch {
                pendingPurchasesLock.withLock {
                    pendingPurchases.remove(productId)?.let { deferred ->
                        pendingPurchasesAcknowledgeFlags.remove(productId)
                        if (!deferred.isCompleted) {
                            deferred.complete(Result.failure(error))
                        }
                    }
                }
            }
        } ?: completeAllPendingPurchasesError(error)
    }

    private fun completeAllPendingPurchasesError(error: IAPError) {
        callbackScope.launch {
            pendingPurchasesLock.withLock {
                pendingPurchases.values.forEach { deferred ->
                    if (!deferred.isCompleted) {
                        deferred.complete(Result.failure(error))
                    }
                }
                pendingPurchases.clear()
                pendingPurchasesAcknowledgeFlags.clear()
            }
        }
    }

    private suspend fun cleanupPendingPurchase(productId: String) {
        pendingPurchasesLock.withLock {
            pendingPurchases.remove(productId)?.let { deferred ->
                pendingPurchasesAcknowledgeFlags.remove(productId)
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
}
