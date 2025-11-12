package com.yral.shared.iap.providers

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.PurchaseState
import com.yral.shared.iap.model.SubscriptionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.yral.shared.iap.model.Purchase as IAPPurchase

internal class PurchaseManager(
    private val connectionManager: BillingClientConnectionManager,
) {
    suspend fun restorePurchases(): Result<List<IAPPurchase>> =
        try {
            val client = connectionManager.ensureReady()
            val purchases = mutableListOf<IAPPurchase>()
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
                        purchases.add(convertPurchase(purchase, productType))
                        acknowledgePurchaseIfNeeded(client, purchase)
                    }
                }
                continuation.resume(billingResult)
            }
        }

    private fun convertPurchase(
        purchase: Purchase,
        productType: String = BillingClient.ProductType.INAPP,
    ): IAPPurchase {
        val isSubscription = productType == BillingClient.ProductType.SUBS
        val isAutoRenewing: Boolean? = if (isSubscription) purchase.isAutoRenewing else null
        val isSuspended: Boolean? = if (isSubscription) purchase.isSuspended else null
        val expirationDate: Long? = null
        val accountIdentifier = purchase.accountIdentifiers?.obfuscatedAccountId

        val subscriptionStatus =
            if (isSubscription) {
                determineSubscriptionStatus(expirationDate, isAutoRenewing, isSuspended)
            } else {
                SubscriptionStatus.UNKNOWN
            }

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
            accountIdentifier = accountIdentifier,
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
