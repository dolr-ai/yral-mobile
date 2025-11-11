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
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.yral.shared.iap.model.Purchase as IAPPurchase

/**
 * Android implementation of IAPProvider using Google Play Billing Library.
 * Orchestrates product fetching, purchases, and purchase restoration for Android devices.
 *
 * Internal implementation - consumers should use [com.yral.shared.iap.IAPManager] instead.
 */
internal class AndroidIAPProvider(
    context: Context,
    appDispatchers: AppDispatchers,
) : IAPProvider {
    // Track pending purchases by product ID
    private val pendingPurchases = mutableMapOf<String, CompletableDeferred<Result<IAPPurchase>>>()
    private val pendingPurchasesLock = Mutex()

    // Coroutine scope for handling purchase callbacks
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

    /**
     * Initiates a purchase flow for the specified product.
     * Requires Activity context on Android to launch the billing flow UI.
     *
     * @param productId Product ID to purchase
     * @param context Activity context (required on Android)
     * @return Result containing Purchase object, or error if purchase fails
     */
    @Suppress("ReturnCount")
    override suspend fun purchaseProduct(
        productId: ProductId,
        context: Any?,
    ): Result<IAPPurchase> {
        return try {
            val productIdString = productId.productId

            // Cast context to Activity
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
                    // Query product details
                    productFetcher.queryProductDetailsForPurchase(productIdString)
                        ?: return Result.failure(IAPError.ProductNotFound(productIdString))
                }

            // Launch billing flow
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
                // Purchase flow started - create deferred to wait for result from PurchasesUpdatedListener
                val deferred = CompletableDeferred<Result<IAPPurchase>>()
                pendingPurchasesLock.withLock {
                    pendingPurchases[productIdString] = deferred
                }

                // Wait for purchase result from PurchasesUpdatedListener
                deferred.await()
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

    /**
     * Checks if a specific product has been purchased and is in PURCHASED state.
     *
     * @param productId Product ID to check
     * @return true if product is purchased, false otherwise
     */
    override suspend fun isProductPurchased(productId: ProductId): Boolean =
        runCatching {
            val productIdString = productId.productId
            val restoreResult = restorePurchases()
            restoreResult
                .getOrNull()
                ?.any { it.productId == productIdString && it.state == PurchaseState.PURCHASED }
                ?: false
        }.getOrDefault(false)

    /**
     * Handles purchase updates from PurchasesUpdatedListener.
     * Completes pending purchase deferreds with the result.
     */
    private fun handlePurchaseUpdate(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            // Handle billing errors - complete all pending purchases
            callbackScope.launch {
                pendingPurchasesLock.withLock {
                    val error =
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

                    // Complete all pending purchases with error
                    pendingPurchases.values.forEach { deferred ->
                        if (!deferred.isCompleted) {
                            deferred.complete(Result.failure(error))
                        }
                    }
                    pendingPurchases.clear()
                }
            }
            return
        }

        // Process successful purchases
        purchases?.forEach { purchase ->
            val productId = purchase.products.firstOrNull() ?: return@forEach
            val iapPurchase = convertPurchase(purchase)

            // Acknowledge purchase if needed
            acknowledgePurchaseIfNeeded(purchase)

            // Complete pending purchase
            callbackScope.launch {
                pendingPurchasesLock.withLock {
                    pendingPurchases.remove(productId)?.complete(Result.success(iapPurchase))
                }
            }
        }
    }

    /**
     * Converts Android Purchase object to IAPPurchase model.
     */
    private fun convertPurchase(purchase: Purchase): IAPPurchase =
        IAPPurchase(
            productId = purchase.products.firstOrNull() ?: "",
            purchaseToken = purchase.purchaseToken,
            purchaseTime = purchase.purchaseTime,
            state =
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
                    Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
                    else -> PurchaseState.FAILED
                },
        )

    /**
     * Acknowledges a purchase if it hasn't been acknowledged yet.
     * Required for non-consumable products and subscriptions.
     */
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
}
