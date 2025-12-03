package com.yral.shared.iap.core

import co.touchlab.kermit.Logger
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.Purchase
import com.yral.shared.iap.core.providers.IAPProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IAPManager(
    private val provider: IAPProvider,
) {
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
        context: Any? = null,
    ): Result<Purchase> {
        val result = provider.purchaseProduct(productId, context)
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

    suspend fun restorePurchases(): Result<List<Purchase>> {
        val result = provider.restorePurchases()
        notifyListeners {
            if (result.isSuccess) {
                result.getOrNull()?.let { purchases ->
                    onPurchasesRestored(purchases)
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

    private suspend fun notifyListeners(action: IAPListener.() -> Unit) {
        val currentListeners = listenersMutex.withLock { listeners.toList() }
        currentListeners.forEach { listener ->
            runCatching {
                listener.action()
            }.onFailure {
                Logger.e("IAPManager", it) {
                    "IAP listener failed while executing action for listener=${listener::class.simpleName}"
                }
            }
        }
    }
}
