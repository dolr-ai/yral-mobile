package com.yral.shared.iap.providers

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.PurchaseState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.yral.shared.iap.model.Purchase as IAPPurchase

/**
 * Handles purchase operations and purchase restoration.
 */
internal class PurchaseManager(
    private val connectionManager: BillingClientConnectionManager,
) {
    /**
     * Restores all previously purchased products (both in-app and subscriptions).
     * Automatically acknowledges unacknowledged purchases.
     *
     * @return Result containing list of restored purchases, or error if restore fails
     */
    suspend fun restorePurchases(): Result<List<IAPPurchase>> =
        try {
            val client = connectionManager.ensureReady()
            val purchases = mutableListOf<IAPPurchase>()

            // Query both in-app and subscription purchases
            val inAppResult = queryPurchases(client, BillingClient.ProductType.INAPP, purchases)
            val subscriptionResult = queryPurchases(client, BillingClient.ProductType.SUBS, purchases)

            if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK ||
                subscriptionResult.responseCode == BillingClient.BillingResponseCode.OK
            ) {
                Result.success(purchases)
            } else {
                Result.failure(
                    IAPError.UnknownError(
                        Exception("Failed to restore purchases"),
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

    /**
     * Queries purchases for a specific product type and adds them to the purchases list.
     * Automatically acknowledges unacknowledged purchases.
     */
    private suspend fun queryPurchases(
        client: BillingClient,
        productType: String,
        purchases: MutableList<IAPPurchase>,
    ): BillingResult =
        suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(productType)
                    .build(),
            ) { billingResult, purchaseList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchaseList.forEach { purchase ->
                        purchases.add(convertPurchase(purchase))
                        acknowledgePurchaseIfNeeded(client, purchase)
                    }
                }
                continuation.resume(billingResult)
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
    private fun acknowledgePurchaseIfNeeded(
        client: BillingClient,
        purchase: Purchase,
    ) {
        if (!purchase.isAcknowledged) {
            val params =
                AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            client.acknowledgePurchase(params) { }
        }
    }
}
