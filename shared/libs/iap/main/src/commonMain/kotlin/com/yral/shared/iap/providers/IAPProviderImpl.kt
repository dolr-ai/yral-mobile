package com.yral.shared.iap.providers

import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.iap.core.util.handleIAPResultOperation
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
        context: Any?,
        acknowledgePurchase: Boolean,
    ): Result<CorePurchase> =
        handleIAPResultOperation {
            sessionManager.userPrincipal?.let { userId ->
                coreProvider
                    .purchaseProduct(
                        productId = productId,
                        context = context,
                        obfuscatedAccountId = userId,
                        acknowledgePurchase = acknowledgePurchase,
                    ).map { purchase ->
                        verificationService.verifyPurchase(purchase, userId).fold(
                            onSuccess = { purchase },
                            onFailure = { error -> throw error },
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

                        rawPurchases.filter { purchase ->
                            verificationService.verifyPurchase(purchase, userId).fold(
                                onSuccess = { verifiedPurchases.add(purchase) },
                                onFailure = { error ->
                                    if (error is IAPError) {
                                        verificationErrors.add(error)
                                    } else {
                                        verificationErrors.add(IAPError.UnknownError(error))
                                    }
                                },
                            )
                        }

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
