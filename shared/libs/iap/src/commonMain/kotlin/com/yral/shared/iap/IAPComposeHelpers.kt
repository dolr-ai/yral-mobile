package com.yral.shared.iap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.Purchase
import kotlinx.coroutines.launch

/**
 * Composable helper that provides platform-specific context for IAP purchases.
 * Returns a lambda that can be called on button click.
 *
 * On Android: Extracts Activity from LocalContext
 * On iOS: Uses Unit (no context needed)
 *
 * Uses [IAPListener] pattern for consistency with the rest of the IAP system.
 * The listener is automatically registered when the composable enters composition
 * and unregistered when it leaves.
 *
 * @param listener IAPListener implementation to handle purchase events
 * @return Lambda function that initiates purchase when called with ProductId
 */
@Composable
fun IAPManager.rememberPurchase(listener: IAPListener): (ProductId) -> Unit {
    val context = getPurchaseContext()
    val scope = rememberCoroutineScope()

    // Register listener when composable enters composition
    DisposableEffect(this, listener) {
        scope.launch {
            addListener(listener)
        }
        onDispose {
            scope.launch {
                removeListener(listener)
            }
        }
    }

    return remember(context) {
        { productId: ProductId ->
            scope.launch {
                purchaseProduct(productId, context)
                // Listener will be notified automatically by purchaseProduct
            }
        }
    }
}

/**
 * Convenience overload that creates an IAPListener from callbacks.
 * This is a simpler API for cases where you only need purchase success/error callbacks.
 *
 * @param onSuccess Callback invoked when purchase succeeds
 * @param onError Callback invoked when purchase fails
 * @return Lambda function that initiates purchase when called with ProductId
 */
@Composable
fun IAPManager.rememberPurchase(
    onSuccess: (Purchase) -> Unit = {},
    onError: (IAPError) -> Unit = {},
): (ProductId) -> Unit {
    val listener =
        remember(onSuccess, onError) {
            object : IAPListener {
                override fun onProductsFetched(products: List<Product>) {
                    // No-op for purchase-only listener
                }

                override fun onPurchaseSuccess(purchase: Purchase) {
                    onSuccess(purchase)
                }

                override fun onPurchaseError(error: IAPError) {
                    onError(error)
                }

                override fun onPurchasesRestored(purchases: List<Purchase>) {
                    // No-op for purchase-only listener
                }

                override fun onRestoreError(error: IAPError) {
                    // No-op for purchase-only listener
                }
            }
        }
    return rememberPurchase(listener)
}

/**
 * Gets platform-specific context for purchases.
 * Android: Returns Activity from LocalContext
 * iOS: Returns Unit
 */
@Composable
internal expect fun getPurchaseContext(): Any?
