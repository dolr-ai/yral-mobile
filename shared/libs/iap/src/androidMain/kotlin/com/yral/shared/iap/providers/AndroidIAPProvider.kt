package com.yral.shared.iap.providers

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.PurchaseState
import com.yral.shared.iap.model.SubscriptionStatus
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import com.yral.shared.iap.model.Purchase as IAPPurchase

private val PURCHASE_TIMEOUT: Duration = 5.minutes

internal class AndroidIAPProvider(
    context: Context,
    appDispatchers: AppDispatchers,
) : IAPProvider {
    private val pendingPurchases = mutableMapOf<String, CompletableDeferred<Result<IAPPurchase>>>()
    private val pendingPurchasesLock = Mutex()
    private val callbackScope = CoroutineScope(SupervisorJob() + appDispatchers.network)

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            handlePurchaseUpdate(billingResult, purchases)
        }

    private val connectionManager = BillingClientConnectionManager(context, purchasesUpdatedListener)
    private val productFetcher = ProductFetcher(connectionManager)
    private val purchaseManager = PurchaseManager(connectionManager)

    override suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        productFetcher
            .fetchProducts(productIds)

    @Suppress("ReturnCount", "LongMethod")
    override suspend fun purchaseProduct(
        productId: ProductId,
        context: Any?,
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
                    productFetcher.queryProductDetailsForPurchase(productIdString)
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
                                .build(),
                        ),
                    ).build()

            val billingResult = client.launchBillingFlow(activity, flowParams)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val deferred = CompletableDeferred<Result<IAPPurchase>>()
                pendingPurchasesLock.withLock {
                    pendingPurchases[productIdString] = deferred
                }
                try {
                    withTimeout(PURCHASE_TIMEOUT) {
                        deferred.await()
                    }
                } catch (e: TimeoutCancellationException) {
                    cleanupPendingPurchase(productIdString)
                    return Result.failure(
                        IAPError.PurchaseFailed(
                            productIdString,
                            Exception(
                                "Purchase operation timed out after ${PURCHASE_TIMEOUT.inWholeSeconds} seconds",
                                e,
                            ),
                        ),
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    cleanupPendingPurchase(productIdString)
                    throw e
                }
            } else {
                Result.failure(
                    IAPError.PurchaseFailed(
                        productIdString,
                        Exception("Failed to launch billing flow: ${billingResult.debugMessage}"),
                    ),
                )
            }
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }
    }

    override suspend fun restorePurchases(): Result<List<IAPPurchase>> = purchaseManager.restorePurchases()

    override suspend fun isProductPurchased(productId: ProductId): Boolean =
        runCatching {
            val productIdString = productId.productId
            val restoreResult = restorePurchases()
            restoreResult
                .getOrNull()
                ?.any { purchase ->
                    purchase.productId == productIdString &&
                        purchase.state == PurchaseState.PURCHASED &&
                        (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                }
                ?: false
        }.getOrDefault(false)

    private fun handlePurchaseUpdate(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            handleBillingError(billingResult, purchases)
            return
        }

        handleSuccessfulPurchases(purchases)
    }

    private fun handleBillingError(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        callbackScope.launch {
            pendingPurchasesLock.withLock {
                val error = createBillingError(billingResult)
                val matchedProductId = purchases?.firstOrNull()?.products?.firstOrNull()

                if (matchedProductId != null && pendingPurchases.containsKey(matchedProductId)) {
                    completeSpecificPurchaseError(matchedProductId, error)
                } else {
                    completeAllPendingPurchasesError(error)
                }
            }
        }
    }

    private fun createBillingError(billingResult: BillingResult): IAPError =
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                IAPError.PurchaseCancelled("")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                IAPError.PurchaseFailed(
                    "",
                    Exception("Item already owned: ${billingResult.debugMessage}"),
                )
            }
            else -> {
                IAPError.PurchaseFailed(
                    "",
                    Exception("Billing error: ${billingResult.debugMessage}"),
                )
            }
        }

    private fun completeSpecificPurchaseError(
        productId: String,
        error: IAPError,
    ) {
        val matchedError =
            when (error) {
                is IAPError.PurchaseCancelled -> IAPError.PurchaseCancelled(productId)
                is IAPError.PurchaseFailed -> error.copy(productId = productId)
                else -> error
            }
        pendingPurchases.remove(productId)?.let { deferred ->
            if (!deferred.isCompleted) {
                deferred.complete(Result.failure(matchedError))
            }
        }
    }

    private fun completeAllPendingPurchasesError(error: IAPError) {
        pendingPurchases.values.forEach { deferred ->
            if (!deferred.isCompleted) {
                deferred.complete(Result.failure(error))
            }
        }
        pendingPurchases.clear()
    }

    private fun handleSuccessfulPurchases(purchases: List<Purchase>?) {
        purchases?.forEach { purchase ->
            val productId = purchase.products.firstOrNull() ?: return@forEach
            val iapPurchase = convertPurchase(purchase)

            acknowledgePurchaseIfNeeded(purchase)

            callbackScope.launch {
                pendingPurchasesLock.withLock {
                    pendingPurchases.remove(productId)?.let { deferred ->
                        if (!deferred.isCompleted) {
                            deferred.complete(Result.success(iapPurchase))
                        }
                    }
                }
            }
        }
    }

    private fun convertPurchase(purchase: Purchase): IAPPurchase {
        val isAutoRenewing: Boolean = purchase.isAutoRenewing
        val isSuspended: Boolean = purchase.isSuspended
        val expirationDate: Long? = null
        val subscriptionStatus = determineSubscriptionStatus(expirationDate, isAutoRenewing, isSuspended)

        return IAPPurchase(
            productId = purchase.products.firstOrNull() ?: "",
            purchaseToken = purchase.purchaseToken,
            purchaseTime = purchase.purchaseTime,
            state =
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
                    Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
                    else -> PurchaseState.FAILED
                },
            expirationDate = expirationDate,
            isAutoRenewing = isAutoRenewing,
            subscriptionStatus = subscriptionStatus,
        )
    }

    @Suppress("ReturnCount")
    private fun determineSubscriptionStatus(
        expirationDate: Long?,
        isAutoRenewing: Boolean?,
        isSuspended: Boolean?,
    ): SubscriptionStatus {
        if (isSuspended == true) return SubscriptionStatus.PAUSED
        expirationDate?.let { expiry ->
            @OptIn(ExperimentalTime::class)
            if (expiry <= Clock.System.now().toEpochMilliseconds()) {
                return SubscriptionStatus.EXPIRED
            }
        }
        if (isAutoRenewing == false) return SubscriptionStatus.CANCELLED
        return SubscriptionStatus.ACTIVE
    }

    private fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            callbackScope.launch {
                val client = connectionManager.ensureReady()
                val params =
                    com.android.billingclient.api.AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                client.acknowledgePurchase(params) { }
            }
        }
    }

    /**
     * Cleans up a pending purchase deferred when operation is cancelled or times out.
     * Ensures no memory leaks from hanging deferreds.
     */
    private suspend fun cleanupPendingPurchase(productId: String) {
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
}
