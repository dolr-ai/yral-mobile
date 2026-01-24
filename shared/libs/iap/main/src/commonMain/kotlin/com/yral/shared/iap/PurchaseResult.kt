package com.yral.shared.iap

sealed class PurchaseResult {
    data object NoPurchase : PurchaseResult()
    data object PurchaseMatches : PurchaseResult()
    data object AccountMismatch : PurchaseResult()
    data object UnaccountedPurchase : PurchaseResult()
}
