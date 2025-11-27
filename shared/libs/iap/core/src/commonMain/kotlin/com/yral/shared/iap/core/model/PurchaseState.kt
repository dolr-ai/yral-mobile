package com.yral.shared.iap.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PurchaseState {
    PENDING,
    PURCHASED,
    FAILED,
}
