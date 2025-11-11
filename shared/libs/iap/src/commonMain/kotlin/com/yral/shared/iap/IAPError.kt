package com.yral.shared.iap

sealed class IAPError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data class ProductNotFound(
        val productId: String,
    ) : IAPError("Product not found: $productId")
    data class PurchaseFailed(
        val productId: String,
        override val cause: Throwable? = null,
    ) : IAPError("Purchase failed for product: $productId", cause)
    data class PurchaseCancelled(
        val productId: String,
    ) : IAPError("Purchase cancelled for product: $productId")
    data class BillingUnavailable(
        override val cause: Throwable? = null,
    ) : IAPError("Billing service unavailable", cause)
    data class NetworkError(
        override val cause: Throwable? = null,
    ) : IAPError("Network error during IAP operation", cause)
    data class UnknownError(
        override val cause: Throwable? = null,
    ) : IAPError("Unknown IAP error", cause)
}
