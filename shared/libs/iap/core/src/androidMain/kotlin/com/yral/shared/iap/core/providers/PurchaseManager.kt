package com.yral.shared.iap.core.providers

import co.touchlab.kermit.Logger
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.iap.core.model.SubscriptionStatus
import com.yral.shared.iap.core.util.handleIAPResultOperation
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.yral.shared.iap.core.model.Purchase as IAPPurchase

internal class PurchaseManager(
    private val connectionManager: BillingClientConnectionManager,
    appDispatchers: AppDispatchers,
) {
    private val acknowledgmentScope = CoroutineScope(SupervisorJob() + appDispatchers.network)
    suspend fun restorePurchases(acknowledgePurchase: Boolean = true): Result<List<IAPPurchase>> =
        handleIAPResultOperation {
            val client = connectionManager.ensureReady()
            val purchases = mutableListOf<IAPPurchase>()
            val inAppResult =
                queryPurchases(
                    client,
                    BillingClient.ProductType.INAPP,
                    purchases,
                    acknowledgePurchase,
                )
            val subscriptionResult =
                queryPurchases(
                    client,
                    BillingClient.ProductType.SUBS,
                    purchases,
                    acknowledgePurchase,
                )

            Logger.d("SubscriptionX") {
                "Restored purchases:" +
                    "iap: ${inAppResult.responseCode}, " +
                    "subs: ${subscriptionResult.responseCode}"
            }

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
        }

    private suspend fun queryPurchases(
        client: BillingClient,
        productType: String,
        purchases: MutableList<IAPPurchase>,
        acknowledgePurchase: Boolean = true,
    ): BillingResult =
        suspendCancellableCoroutine { continuation ->
            val unacknowledgedPurchases = mutableListOf<Purchase>()

            client.queryPurchasesAsync(
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(productType)
                    .build(),
            ) { billingResult, purchaseList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchaseList.forEach { purchase ->
                        purchases.add(convertPurchase(purchase, productType))
                        // Collect purchases that need acknowledgment instead of acknowledging immediately
                        if (acknowledgePurchase && !purchase.isAcknowledged) {
                            unacknowledgedPurchases.add(purchase)
                        }
                    }
                }
                // Resume immediately without blocking the billing callback
                continuation.resume(billingResult)

                // After resuming, acknowledge purchases asynchronously (only if acknowledgePurchase is true)
                if (acknowledgePurchase && unacknowledgedPurchases.isNotEmpty()) {
                    Logger.d("SubscriptionX") { "Acknowledging restored purchases $unacknowledgedPurchases" }
                    acknowledgmentScope.launch {
                        unacknowledgedPurchases.forEach { purchase ->
                            try {
                                acknowledgePurchaseIfNeeded(client, purchase)
                            } catch (e: TimeoutCancellationException) {
                                throw e
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                // The acknowledgment failure is handled in acknowledgePurchaseIfNeeded callback
                            }
                        }
                    }
                }
            }
        }

    fun convertPurchase(
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
            productId = ProductId.fromString(purchase.products.firstOrNull() ?: ""),
            purchaseToken = purchase.purchaseToken,
            purchaseTime = purchase.purchaseTime,
            state =
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
                    Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
                    else -> PurchaseState.UNSPECIFIED
                },
            expirationDate = expirationDate,
            isAutoRenewing = isAutoRenewing,
            subscriptionStatus = subscriptionStatus,
            accountIdentifier = accountIdentifier,
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun determineSubscriptionStatus(
        expirationDate: Long?,
        isAutoRenewing: Boolean?,
        isSuspended: Boolean?,
    ): SubscriptionStatus =
        when {
            isSuspended == true -> SubscriptionStatus.PAUSED
            expirationDate != null && expirationDate <= Clock.System.now().toEpochMilliseconds() ->
                SubscriptionStatus.EXPIRED
            isAutoRenewing == null && expirationDate == null -> SubscriptionStatus.UNKNOWN
            isAutoRenewing == false -> SubscriptionStatus.CANCELLED
            else -> SubscriptionStatus.ACTIVE
        }

    suspend fun acknowledgePurchaseIfNeeded(
        client: BillingClient,
        purchase: Purchase,
    ) {
        Logger.d("SubscriptionX") { "acknowledgePurchaseIfNeeded ${purchase.isAcknowledged}" }
        if (!purchase.isAcknowledged) {
            val params =
                AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            suspendCancellableCoroutine { continuation ->
                client.acknowledgePurchase(params) { billingResult ->
                    // Handle acknowledgment result
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        // Log error if needed - acknowledgment failure doesn't block the restore operation
                        Logger.d("SubscriptionX") { "Failed to acknowledge purchase" }
                    }
                    // Resume normally regardless of response code to allow callers to await completion
                    continuation.resume(Unit)
                }
                // Handle coroutine cancellation
                continuation.invokeOnCancellation {
                    // Clean up if needed - acknowledgePurchase callback will still be invoked
                    // but we won't wait for it if the coroutine is cancelled
                }
            }
        }
    }
}
