package com.yral.shared.iap

import co.touchlab.kermit.Logger
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.Purchase
import com.yral.shared.iap.providers.IAPProvider
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
        notifyListeners {
            if (result.isSuccess) {
                result.getOrNull()?.let { products ->
                    onProductsFetched(products)
                }
            }
        }
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

    suspend fun restorePurchases(userId: String?): Result<List<Purchase>> {
        val result = provider.restorePurchases(userId)
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

    suspend fun isProductPurchased(
        productId: ProductId,
        userId: String?,
    ): Result<Boolean> = provider.isProductPurchased(productId, userId)

    suspend fun setAccountIdentifier(
        userId: String,
        accountIdentifier: String,
    ) {
        provider.setAccountIdentifier(userId, accountIdentifier)
    }

    suspend fun notifyWarning(message: String) {
        Logger.w("IAPManager") { message }
        notifyListeners {
            onWarning(message)
        }
    }

    private suspend fun notifyListeners(action: IAPListener.() -> Unit) {
        listenersMutex.withLock {
            listeners.forEach { listener ->
                runCatching {
                    listener.action()
                }.onFailure {
                    Logger.e("IAPManager", it) { "Failed to notify listener ${listener::class}" }
                }
            }
        }
    }
}
