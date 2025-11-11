package com.yral.shared.iap.providers

import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.Purchase

/** Internal interface for platform-specific IAP implementations. Use IAPManager instead. */
interface IAPProvider {
    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>>
    suspend fun purchaseProduct(
        productId: ProductId,
        context: Any? = null,
    ): Result<Purchase>
    suspend fun restorePurchases(): Result<List<Purchase>>
    suspend fun isProductPurchased(productId: ProductId): Boolean
}
