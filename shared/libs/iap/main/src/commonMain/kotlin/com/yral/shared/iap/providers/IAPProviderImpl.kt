package com.yral.shared.iap.providers

import co.touchlab.kermit.Logger
import com.yral.shared.core.session.DEFAULT_DAYS
import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.PurchaseResult
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.iap.core.util.handleIAPResultOperation
import com.yral.shared.iap.utils.PurchaseContext
import com.yral.shared.iap.utils.toPlatformContext
import com.yral.shared.iap.verification.PurchaseVerificationService
import kotlin.time.Duration.Companion.days
import com.yral.shared.iap.core.model.Purchase as CorePurchase
import com.yral.shared.iap.core.providers.IAPProvider as CoreIAPProvider

internal class IAPProviderImpl(
    private val coreProvider: CoreIAPProvider,
    private val sessionManager: SessionManager,
    private val verificationService: PurchaseVerificationService,
) : IAPProvider {
    private var warningNotifier: (String) -> Unit = {}

    fun setWarningNotifier(notifier: (String) -> Unit) {
        warningNotifier = notifier
    }

    override suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        coreProvider
            .fetchProducts(productIds)

    override suspend fun purchaseProduct(
        productId: ProductId,
        context: PurchaseContext?,
        acknowledgePurchase: Boolean,
    ): Result<CorePurchase> =
        handleIAPResultOperation {
            sessionManager.userPrincipal?.let { userId ->
                coreProvider
                    .purchaseProduct(
                        productId = productId,
                        context = context.toPlatformContext(),
                        obfuscatedAccountId = userId,
                        acknowledgePurchase = acknowledgePurchase,
                    ).map { purchase ->
                        Logger.d("SubscriptionX") { "verify purchase $purchase" }
                        verificationService.verifyPurchase(purchase, userId).fold(
                            onSuccess = {
                                Logger.d("SubscriptionXM") { "purchase acknowledged" }
                                purchase
                            },
                            onFailure = { error ->
                                Logger.e("SubscriptionXM", error) { "purchase acknowledge failed" }
                                throw error
                            },
                        )
                    }
            } ?: throw IAPError.UnknownError(Exception("User principal is null"))
        }

    override suspend fun restorePurchases(
        acknowledgePurchase: Boolean,
        verifyPurchases: Boolean,
    ): Result<RestoreResult> =
        handleIAPResultOperation {
            sessionManager.userPrincipal?.let { userId ->
                coreProvider
                    .restorePurchases(acknowledgePurchase = acknowledgePurchase)
                    .map { rawPurchases ->
                        if (verifyPurchases) {
                            val verifiedPurchases = mutableListOf<CorePurchase>()
                            val verificationErrors = mutableListOf<IAPError>()
                            Logger.d("SubscriptionXM") { "verify restored purchases $rawPurchases" }
                            rawPurchases.filter { purchase ->
                                verificationService.verifyPurchase(purchase, userId).fold(
                                    onSuccess = {
                                        Logger.d("SubscriptionXM") { "purchase verified $purchase" }
                                        verifiedPurchases.add(purchase)
                                    },
                                    onFailure = { error ->
                                        Logger.e("SubscriptionXM", error) { "purchase verification failed $error" }
                                        if (error is IAPError) {
                                            verificationErrors.add(error)
                                        } else {
                                            verificationErrors.add(IAPError.UnknownError(error))
                                        }
                                    },
                                )
                            }
                            Logger.d("SubscriptionXM") { "verified purchases $verifiedPurchases" }
                            RestoreResult(verifiedPurchases, verificationErrors)
                        } else {
                            Logger.d("SubscriptionXM") { "restored purchases $rawPurchases" }
                            RestoreResult(rawPurchases, emptyList())
                        }
                    }
            } ?: throw IAPError.UnknownError(Exception("User principal is null"))
        }

    override suspend fun isProductPurchased(productId: ProductId): Result<PurchaseResult> =
        handleIAPResultOperation {
            sessionManager.userPrincipal?.let { userId ->
                restorePurchases(verifyPurchases = false)
                    .map { result ->
                        result.purchases.firstOrNull { purchase ->
                            purchase.productId == productId &&
                                purchase.state == PurchaseState.PURCHASED &&
                                (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                        }
                    }.map { purchase ->
                        when {
                            purchase == null -> PurchaseResult.NoPurchase
                            purchase.accountIdentifier == null -> PurchaseResult.UnaccountedPurchase
                            purchase.accountIdentifier != userId -> PurchaseResult.AccountMismatch
                            else ->
                                PurchaseResult.PurchaseMatches(
                                    validTill = purchase.purchaseTime + DEFAULT_DAYS.days.inWholeMilliseconds,
                                )
                        }
                    }
            } ?: throw IAPError.UnknownError(Exception("User principal is null"))
        }
}
