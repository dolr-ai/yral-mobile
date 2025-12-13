package com.yral.shared.iap.core

import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.Purchase

interface IAPListener {
    fun onProductsFetched(products: List<Product>)
    fun onProductsError(error: IAPError)
    fun onPurchaseSuccess(purchase: Purchase)
    fun onPurchaseError(error: IAPError)
    fun onPurchasesRestored(purchases: List<Purchase>)
    fun onRestoreError(error: IAPError)
}
