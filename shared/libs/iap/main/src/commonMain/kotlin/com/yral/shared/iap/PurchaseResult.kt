package com.yral.shared.iap

sealed class PurchaseResult {
    data object NoPurchase : PurchaseResult()
    data class PurchaseMatches(
        val validTill: Long,
    ) : PurchaseResult()
    data object AccountMismatch : PurchaseResult()
    data object UnaccountedPurchase : PurchaseResult()
}
