package com.yral.shared.iap.core.providers

interface AppleStoreKitBridge {
    fun purchase(
        productId: String,
        appAccountToken: String,
        completion: (AppleStoreKitPurchaseResult?, String?) -> Unit,
    )

    fun unfinishedPurchases(completion: (List<AppleStoreKitPurchaseResult>?, String?) -> Unit)

    fun finish(
        transactionId: String,
        completion: (String?) -> Unit,
    )
}

data class AppleStoreKitPurchaseResult(
    val productId: String,
    val transactionId: String,
    val purchaseTime: Long,
)
