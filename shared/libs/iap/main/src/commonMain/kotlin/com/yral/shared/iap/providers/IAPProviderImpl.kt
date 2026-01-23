package com.yral.shared.iap.providers

import co.touchlab.kermit.Logger
import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.iap.core.util.handleIAPResultOperation
import com.yral.shared.iap.utils.PurchaseContext
import com.yral.shared.iap.utils.toPlatformContext
import com.yral.shared.iap.verification.PurchaseVerificationService
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

    override suspend fun restorePurchases(acknowledgePurchase: Boolean): Result<RestoreResult> =
        handleIAPResultOperation {
            sessionManager.userPrincipal?.let { userId ->
                coreProvider
                    .restorePurchases(acknowledgePurchase = acknowledgePurchase)
                    .map { rawPurchases ->
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
                    }
            } ?: throw IAPError.UnknownError(Exception("User principal is null"))
        }

    override suspend fun isProductPurchased(productId: ProductId): Result<Boolean> =
        restorePurchases().map { result ->
            result.purchases.any { purchase ->
                purchase.productId == productId &&
                    purchase.state == PurchaseState.PURCHASED &&
                    (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
            }
        }
}
