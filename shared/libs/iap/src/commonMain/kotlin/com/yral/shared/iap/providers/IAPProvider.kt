package com.yral.shared.iap.providers

import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.Purchase

/**
 * Interface for platform-specific IAP implementations.
 *
 * **Note:** This is an internal implementation detail. Consumers should use [IAPManager] instead.
 * This interface is kept public for dependency injection purposes only.
 */
interface IAPProvider {
    /**
     * Fetches products from the platform store
     * @param productIds List of product IDs to fetch
     * @return Result containing list of products or IAPError
     */
    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>>

    /**
     * Initiates a purchase for the given product
     * @param productId The product ID to purchase
     * @param context Platform-specific context (Activity on Android, Unit on iOS)
     * @return Result containing Purchase or IAPError
     */
    suspend fun purchaseProduct(
        productId: ProductId,
        context: Any? = null,
    ): Result<Purchase>

    /**
     * Restores previously purchased products
     * @return Result containing list of restored purchases or IAPError
     */
    suspend fun restorePurchases(): Result<List<Purchase>>

    /**
     * Checks if a product is currently purchased
     * @param productId The product ID to check
     * @return true if the product is purchased, false otherwise
     */
    suspend fun isProductPurchased(productId: ProductId): Boolean
}
