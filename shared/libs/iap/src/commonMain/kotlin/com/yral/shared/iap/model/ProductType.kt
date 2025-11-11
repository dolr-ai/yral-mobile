package com.yral.shared.iap.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProductType {
    CONSUMABLE,
    NON_CONSUMABLE,
    SUBSCRIPTION,
}
