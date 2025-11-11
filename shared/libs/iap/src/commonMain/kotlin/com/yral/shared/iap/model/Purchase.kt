package com.yral.shared.iap.model

import kotlinx.serialization.Serializable

@Serializable
data class Purchase(
    val productId: String,
    // Android purchase token
    val purchaseToken: String? = null,
    // iOS receipt
    val receipt: String? = null,
    val purchaseTime: Long,
    val state: PurchaseState,
)
