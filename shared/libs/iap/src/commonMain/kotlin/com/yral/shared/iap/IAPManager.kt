package com.yral.shared.iap

import co.touchlab.kermit.Logger
import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.Purchase
import com.yral.shared.iap.providers.IAPProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IAPManager(
    private val provider: IAPProvider,
) {
    private val listeners = mutableSetOf<IAPListener>()
    private val listenersMutex = Mutex()

    /**
     * Registers a listener to receive IAP events
     * Note: This is thread-safe and can be called from any thread
     */
    suspend fun addListener(listener: IAPListener) {
        listenersMutex.withLock {
            listeners.add(listener)
        }
    }

    /**
     * Unregisters a listener
     * Note: This is thread-safe and can be called from any thread
     */
    suspend fun removeListener(listener: IAPListener) {
        listenersMutex.withLock {
            listeners.remove(listener)
        }
    }

    /**
     * Fetches products from the platform store
     */
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

    /**
     * Initiates a purchase for the given product.
     *
     * Note: In Compose, prefer using [rememberPurchase] instead.
     * This method is kept for non-Compose usage or advanced scenarios.
     *
     * @param productId The product ID to purchase
     * @param context Platform-specific context (Activity on Android, Unit on iOS)
     */
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

    /**
     * Restores previously purchased products
     */
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

    /**
     * Checks if a product is currently purchased
     */
    suspend fun isProductPurchased(productId: ProductId): Boolean = provider.isProductPurchased(productId)

    private suspend fun notifyListeners(action: IAPListener.() -> Unit) {
        listenersMutex.withLock {
            listeners.forEach { listener ->
                runCatching {
                    listener.action()
                }.onFailure {
                    // Log error but don't break other listeners
                    // In production, you might want to use a logger here
                    Logger.e("IAPManager", it) { "Failed to notify listener ${listener::class}" }
                }
            }
        }
    }
}
