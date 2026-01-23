package com.yral.shared.iap.providers

import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.Purchase
import com.yral.shared.iap.utils.PurchaseContext

interface IAPProvider {
    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>>

    /**
     * Initiates a purchase flow for the specified product.
     *
     * @param productId The identifier of the product to purchase.
     * @param context Purchase context from [com.yral.shared.iap.utils.getPurchaseContext]. Required on Android.
     * @param acknowledgePurchase If true, automatically acknowledges the purchase. Defaults to false.
     * @return A [Result] containing the [Purchase] on success, or an error on failure.
     */
    suspend fun purchaseProduct(
        productId: ProductId,
        context: PurchaseContext? = null,
        acknowledgePurchase: Boolean = false,
    ): Result<Purchase>
    suspend fun restorePurchases(
        acknowledgePurchase: Boolean = false,
        verifyPurchases: Boolean = true,
    ): Result<RestoreResult>
    suspend fun isProductPurchased(productId: ProductId): Result<Boolean>
    suspend fun queryPurchase(productId: ProductId): Result<Purchase?>
}
