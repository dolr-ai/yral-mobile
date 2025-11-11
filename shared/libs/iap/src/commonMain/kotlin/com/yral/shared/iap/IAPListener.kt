package com.yral.shared.iap

import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.Purchase

interface IAPListener {
    /**
     * Called when products are successfully fetched
     */
    fun onProductsFetched(products: List<Product>)

    /**
     * Called when a purchase is successful
     */
    fun onPurchaseSuccess(purchase: Purchase)

    /**
     * Called when a purchase fails
     */
    fun onPurchaseError(error: IAPError)

    /**
     * Called when purchases are successfully restored
     */
    fun onPurchasesRestored(purchases: List<Purchase>)

    /**
     * Called when restore purchases fails
     */
    fun onRestoreError(error: IAPError)
}
