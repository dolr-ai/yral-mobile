package com.yral.shared.iap

import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.Purchase

interface IAPListener {
    fun onProductsFetched(products: List<Product>)
    fun onPurchaseSuccess(purchase: Purchase)
    fun onPurchaseError(error: IAPError)
    fun onPurchasesRestored(purchases: List<Purchase>)
    fun onRestoreError(error: IAPError)
    fun onWarning(message: String)
}
