package com.yral.shared.iap.providers

import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.Purchase

interface IAPProvider {
    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>>
    suspend fun purchaseProduct(
        productId: ProductId,
        context: Any? = null,
        acknowledgePurchase: Boolean = false,
    ): Result<Purchase>
    suspend fun restorePurchases(acknowledgePurchase: Boolean = false): Result<RestoreResult>
    suspend fun isProductPurchased(productId: ProductId): Result<Boolean>
}
