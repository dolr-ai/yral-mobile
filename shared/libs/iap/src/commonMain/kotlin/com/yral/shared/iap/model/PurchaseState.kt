package com.yral.shared.iap.model

import kotlinx.serialization.Serializable

@Serializable
enum class PurchaseState {
    PENDING,
    PURCHASED,
    FAILED,
}
