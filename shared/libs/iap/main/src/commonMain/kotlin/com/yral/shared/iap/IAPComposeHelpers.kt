package com.yral.shared.iap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.Purchase
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@Composable
fun IAPManager.rememberPurchase(listener: IAPListener): (ProductId) -> Unit {
    val context = getPurchaseContext()
    val scope = rememberCoroutineScope()
    LaunchedEffect(this, listener) {
        try {
            addListener(listener)
            awaitCancellation()
        } finally {
            removeListener(listener)
        }
    }
    return remember(context) {
        { productId: ProductId ->
            scope.launch {
                purchaseProduct(productId, context)
            }
        }
    }
}

@Composable
fun IAPManager.rememberPurchase(
    onSuccess: (Purchase) -> Unit = {},
    onError: (IAPError) -> Unit = {},
): (ProductId) -> Unit {
    val listener =
        remember(onSuccess, onError) {
            object : IAPListener {
                override fun onProductsFetched(products: List<Product>) { /* No op */ }
                override fun onProductsError(error: IAPError) { /* No op */ }
                override fun onPurchaseSuccess(purchase: Purchase) = onSuccess(purchase)
                override fun onPurchaseError(error: IAPError) = onError(error)
                override fun onPurchasesRestored(purchases: List<Purchase>) { /* No op */ }
                override fun onRestoreError(error: IAPError) { /* No op */ }
                override fun onWarning(message: String) { /* No op */ }
            }
        }
    return rememberPurchase(listener)
}

@Composable
internal expect fun getPurchaseContext(): Any?
