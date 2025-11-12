package com.yral.shared.iap.providers

import android.app.Activity
import android.content.Context
import co.touchlab.kermit.Logger
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.PurchaseState
import com.yral.shared.iap.model.SubscriptionStatus
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.Preferences
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
private const val ACCOUNT_IDENTIFIER_PREFIX = "iap_account_identifier_"
private const val ACCOUNT_IDENTIFIER_SEPARATOR = ","

internal class AndroidIAPProvider(
    context: Context,
    appDispatchers: AppDispatchers,
    private val preferences: Preferences,
    private val sessionManager: SessionManager,
) : IAPProvider {
    private var warningNotifier: suspend (String) -> Unit = {}

    fun setWarningNotifier(notifier: suspend (String) -> Unit) {
        warningNotifier = notifier
    }
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

    override suspend fun restorePurchases(userId: String?): Result<List<IAPPurchase>> {
        val allPurchases = purchaseManager.restorePurchases()
        return if (userId == null) {
            allPurchases
        } else {
            val accountIdentifiers = getAccountIdentifiersForUser(userId)
            if (accountIdentifiers.isEmpty()) {
                Result.success(emptyList())
            } else {
                allPurchases.map { purchases ->
                    purchases.filter { purchase ->
                        purchase.accountIdentifier != null &&
                            accountIdentifiers.contains(purchase.accountIdentifier)
                    }
                }
            }
        }
    }

    override suspend fun isProductPurchased(
        productId: ProductId,
        userId: String?,
    ): Boolean =
        runCatching {
            val productIdString = productId.productId
            val restoreResult = restorePurchases(userId)
            val purchases = restoreResult.getOrNull() ?: return false

            if (userId == null) {
                return purchases.any { purchase ->
                    purchase.productId == productIdString &&
                        purchase.state == PurchaseState.PURCHASED &&
                        (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                }
            }

            val accountIdentifiers = getAccountIdentifiersForUser(userId)
            if (accountIdentifiers.isEmpty()) {
                return false
            }

            purchases
                .filter { purchase ->
                    purchase.accountIdentifier != null &&
                        accountIdentifiers.contains(purchase.accountIdentifier)
                }.any { purchase ->
                    purchase.productId == productIdString &&
                        purchase.state == PurchaseState.PURCHASED &&
                        (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                }
        }.getOrDefault(false)

    private suspend fun getAccountIdentifiersForUser(userId: String): Set<String> {
        val key = "$ACCOUNT_IDENTIFIER_PREFIX$userId"
        val stored = preferences.getString(key) ?: return emptySet()
        return stored.split(ACCOUNT_IDENTIFIER_SEPARATOR).filter { it.isNotEmpty() }.toSet()
    }

    private suspend fun addAccountIdentifierForUser(
        userId: String,
        accountIdentifier: String,
    ) {
        val existing = getAccountIdentifiersForUser(userId)
        if (!existing.contains(accountIdentifier)) {
            val key = "$ACCOUNT_IDENTIFIER_PREFIX$userId"
            val updated = (existing + accountIdentifier).joinToString(ACCOUNT_IDENTIFIER_SEPARATOR)
            preferences.putString(key, updated)
        }
    }

    private suspend fun validateAccountIdentifier(accountIdentifier: String): Boolean {
        val allPurchases = purchaseManager.restorePurchases().getOrNull() ?: return false
        return allPurchases.any { it.accountIdentifier == accountIdentifier }
    }

    override suspend fun setAccountIdentifier(
        userId: String,
        accountIdentifier: String,
    ) {
        if (validateAccountIdentifier(accountIdentifier)) {
            addAccountIdentifierForUser(userId, accountIdentifier)
        } else {
            val message =
                "Account identifier $accountIdentifier does not match any purchases. " +
                    "Not storing mapping for user $userId."
            Logger.w("AndroidIAPProvider") { message }
            callbackScope.launch {
                warningNotifier(message)
            }
        }
    }

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
            val accountIdentifier = iapPurchase.accountIdentifier

            acknowledgePurchaseIfNeeded(purchase)

            if (accountIdentifier != null) {
                val userId = sessionManager.userPrincipal
                if (userId != null) {
                    callbackScope.launch {
                        val existing = getAccountIdentifiersForUser(userId)
                        if (!existing.contains(accountIdentifier)) {
                            if (existing.isNotEmpty()) {
                                val message =
                                    "Account identifier changed for user $userId. " +
                                        "Existing: ${existing.joinToString()}, " +
                                        "New: $accountIdentifier. " +
                                        "Adding new identifier to support multiple accounts."
                                Logger.w("AndroidIAPProvider") { message }
                                warningNotifier(message)
                            }
                            addAccountIdentifierForUser(userId, accountIdentifier)
                        }
                    }
                } else {
                    val message =
                        "Purchase completed but userId is null. " +
                            "Account identifier $accountIdentifier not stored. " +
                            "User may need to restore purchases after login."
                    Logger.w("AndroidIAPProvider") { message }
                    callbackScope.launch {
                        warningNotifier(message)
                    }
                }
            } else {
                val message =
                    "Purchase completed but account identifier is null. " +
                        "Mapping not stored. User may need backend rehydration."
                Logger.w("AndroidIAPProvider") { message }
                callbackScope.launch {
                    warningNotifier(message)
                }
            }

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
        val accountIdentifier = purchase.accountIdentifiers?.obfuscatedAccountId

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
