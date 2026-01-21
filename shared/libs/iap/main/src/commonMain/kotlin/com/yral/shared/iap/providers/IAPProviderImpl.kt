package com.yral.shared.iap.providers

import co.touchlab.kermit.Logger
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
                    ).mapCatching { purchase ->
                        verificationService.verifyPurchase(purchase, userId).fold(
                            onSuccess = { purchase },
                            onFailure = { error -> throw error },
                        )
                    }
            } ?: throw IAPError.UnknownError(Exception("User principal is null"))
        }

    override suspend fun restorePurchases(
        userId: String?,
        acknowledgePurchase: Boolean,
    ): Result<List<CorePurchase>> =
        handleIAPResultOperation {
            userId?.let {
                coreProvider
                    .restorePurchases(acknowledgePurchase = acknowledgePurchase)
                    .mapCatching { purchases ->
                        purchases.filter { purchase ->
                            verificationService.verifyPurchase(purchase, userId).fold(
                                onSuccess = { true },
                                onFailure = { error ->
                                    Logger.w("IAPProviderImpl", error) {
                                        "Purchase verification error for product ${purchase.productId} during restore"
                                    }
                                    false
                                },
                            )
                        }
                    }
            } ?: throw IAPError.UnknownError(Exception("User principal is null"))
        }

    override suspend fun isProductPurchased(
        productId: ProductId,
        userId: String?,
    ): Result<Boolean> =
        handleIAPResultOperation {
            restorePurchases(userId).map { purchases ->
                purchases.any { purchase ->
                    purchase.productId == productId &&
                        purchase.state == PurchaseState.PURCHASED &&
                        (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                }
            }
        }
}
