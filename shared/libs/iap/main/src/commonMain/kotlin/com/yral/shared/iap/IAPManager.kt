package com.yral.shared.iap

import co.touchlab.kermit.Logger
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.Purchase
import com.yral.shared.iap.providers.IAPProvider
import com.yral.shared.iap.providers.RestoreResult
import com.yral.shared.iap.utils.PurchaseContext
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IAPManager(
    private val provider: IAPProvider,
    appDispatchers: AppDispatchers,
) {
    private val managerScope = CoroutineScope(SupervisorJob() + appDispatchers.network)
    private val listeners = mutableSetOf<IAPListener>()
    private val listenersMutex = Mutex()

    suspend fun addListener(listener: IAPListener) {
        listenersMutex.withLock {
            listeners.add(listener)
        }
    }

    suspend fun removeListener(listener: IAPListener) {
        listenersMutex.withLock {
            listeners.remove(listener)
        }
    }

    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> {
        val result = provider.fetchProducts(productIds)
        result.fold(
            onSuccess = { notifyListeners { onProductsFetched(it) } },
            onFailure = {
                notifyListeners {
                    val error = it as? IAPError ?: IAPError.UnknownError(it)
                    onProductsError(error)
                }
            },
        )
        return result
    }

    suspend fun purchaseProduct(
        productId: ProductId,
        context: PurchaseContext? = null,
        acknowledgePurchase: Boolean = false,
    ): Result<Purchase> {
        Logger.d("SubscriptionXM") { "purchaseProduct $productId" }
        val result = provider.purchaseProduct(productId, context, acknowledgePurchase)
        Logger.d("SubscriptionXM") { "purchaseProduct $result" }
        notifyListeners {
            if (result.isSuccess) {
                result.getOrNull()?.let { purchase ->
                    onPurchaseSuccess(purchase)
                }
            } else {
                val error =
                    result.exceptionOrNull() as? IAPError
                        ?: IAPError.UnknownError(result.exceptionOrNull())
                onPurchaseError(error)
            }
        }
        return result
    }

    suspend fun restorePurchases(acknowledgePurchase: Boolean = false): Result<RestoreResult> {
        Logger.d("SubscriptionXM") { "restorePurchases $acknowledgePurchase" }
        val result = provider.restorePurchases(acknowledgePurchase)
        Logger.d("SubscriptionXM") { "restorePurchases $result" }
        notifyListeners {
            if (result.isSuccess) {
                result.getOrNull()?.let { purchases ->
                    onPurchasesRestored(purchases.purchases)
                    if (purchases.verificationErrors.isNotEmpty()) {
                        onRestoreError(purchases.verificationErrors)
                    }
                }
            } else {
                val error =
                    result.exceptionOrNull() as? IAPError
                        ?: IAPError.UnknownError(result.exceptionOrNull())
                onRestoreError(error)
            }
        }
        return result
    }

    suspend fun isProductPurchased(productId: ProductId): Result<Boolean> = provider.isProductPurchased(productId)

    suspend fun queryPurchase(productId: ProductId): Result<Purchase?> = provider.queryPurchase(productId)

    fun notifyWarning(message: String) {
        Logger.w("SubscriptionXM") { message }
        managerScope.launch {
            notifyListeners { onWarning(message) }
        }
    }

    private suspend fun notifyListeners(action: IAPListener.() -> Unit) {
        val currentListeners = listenersMutex.withLock { listeners.toList() }
        currentListeners.forEach { listener ->
            runCatching {
                listener.action()
            }.onFailure {
                Logger.e("SubscriptionXM", it) { "Failed to notify listener ${listener::class}" }
            }
        }
    }
}
