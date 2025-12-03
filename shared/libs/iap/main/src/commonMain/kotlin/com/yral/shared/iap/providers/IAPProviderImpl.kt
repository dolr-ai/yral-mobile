package com.yral.shared.iap.providers

import co.touchlab.kermit.Logger
import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.yral.shared.iap.core.model.Purchase as CorePurchase
import com.yral.shared.iap.core.providers.IAPProvider as CoreIAPProvider

private const val ACCOUNT_IDENTIFIER_PREFIX = "iap_account_identifier_"
private const val ACCOUNT_IDENTIFIER_SEPARATOR = ","

internal class IAPProviderImpl(
    private val coreProvider: CoreIAPProvider,
    appDispatchers: AppDispatchers,
    private val preferences: Preferences,
    private val sessionManager: SessionManager,
) : IAPProvider {
    private var warningNotifier: suspend (String) -> Unit = {}

    fun setWarningNotifier(notifier: suspend (String) -> Unit) {
        warningNotifier = notifier
    }

    private val callbackScope = CoroutineScope(SupervisorJob() + appDispatchers.network)

    override suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        coreProvider
            .fetchProducts(productIds)

    override suspend fun purchaseProduct(
        productId: ProductId,
        context: Any?,
    ): Result<CorePurchase> {
        val result = coreProvider.purchaseProduct(productId, context)
        result.onSuccess { purchase ->
            val accountIdentifier = purchase.accountIdentifier
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
                                Logger.w("IAPProviderImpl") { message }
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
                    Logger.w("IAPProviderImpl") { message }
                    callbackScope.launch {
                        warningNotifier(message)
                    }
                }
            } else {
                val message =
                    "Purchase completed but account identifier is null. " +
                        "Mapping not stored. User may need backend rehydration."
                Logger.w("IAPProviderImpl") { message }
                callbackScope.launch {
                    warningNotifier(message)
                }
            }
        }
        return result
    }

    override suspend fun restorePurchases(userId: String?): Result<List<CorePurchase>> {
        val allPurchases = coreProvider.restorePurchases()
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
    ): Result<Boolean> =
        try {
            val productIdString = productId.productId
            val restoreResult = restorePurchases(userId)
            restoreResult.map { purchases ->
                if (userId == null) {
                    purchases.any { purchase ->
                        purchase.productId == productIdString &&
                            purchase.state == PurchaseState.PURCHASED &&
                            (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                    }
                } else {
                    val accountIdentifiers = getAccountIdentifiersForUser(userId)
                    if (accountIdentifiers.isEmpty()) {
                        false
                    } else {
                        purchases
                            .filter { purchase ->
                                purchase.accountIdentifier != null &&
                                    accountIdentifiers.contains(purchase.accountIdentifier)
                            }.any { purchase ->
                                purchase.productId == productIdString &&
                                    purchase.state == PurchaseState.PURCHASED &&
                                    (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                            }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
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
            Logger.w("IAPProviderImpl") { message }
            callbackScope.launch {
                warningNotifier(message)
            }
        }
    }

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
        val allPurchases = coreProvider.restorePurchases().getOrNull() ?: return false
        return allPurchases.any { it.accountIdentifier == accountIdentifier }
    }
}
