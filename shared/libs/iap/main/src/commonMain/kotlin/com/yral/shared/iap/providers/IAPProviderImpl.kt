package com.yral.shared.iap.providers

import co.touchlab.kermit.Logger
import com.yral.shared.core.session.SessionManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.iap.verification.PurchaseVerificationService
import kotlin.coroutines.cancellation.CancellationException
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
    ): Result<CorePurchase> =
        try {
            sessionManager.userPrincipal?.let { userId ->
                coreProvider
                    .purchaseProduct(
                        productId = productId,
                        context = context,
                        obfuscatedAccountId = userId,
                    ).mapCatching { purchase ->
                        if (verificationService.verifyPurchase(purchase, userId)) {
                            Logger.w("IAPProviderImpl") {
                                "Purchase verification failed for product ${purchase.productId}"
                            }
                            purchase
                        } else {
                            throw IAPError.PurchaseFailed(purchase.productId)
                        }
                    }
            } ?: throw IAPError.UnknownError(Exception("userId null"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }

    override suspend fun restorePurchases(userId: String?): Result<List<CorePurchase>> =
        try {
            userId?.let {
                coreProvider
                    .restorePurchases()
                    .mapCatching { purchases ->
                        purchases.filter { purchase -> verify(purchase, userId) }
                    }
            } ?: throw IAPError.UnknownError(Exception("userId null"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }

    override suspend fun isProductPurchased(
        productId: ProductId,
        userId: String?,
    ): Result<Boolean> =
        try {
            val productIdString = productId.productId
            restorePurchases(userId).map { purchases ->
                purchases.any { purchase ->
                    purchase.productId == productIdString &&
                        purchase.state == PurchaseState.PURCHASED &&
                        (purchase.subscriptionStatus == null || purchase.isActiveSubscription())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }

    private suspend fun verify(
        purchase: CorePurchase,
        userId: String,
    ): Boolean =
        verificationService
            .verifyPurchase(purchase, userId)
            .also { verified ->
                if (!verified) {
                    Logger.w("IAPProviderImpl") {
                        "Purchase verification failed for product ${purchase.productId} during restore"
                    }
                }
            }
}
