package com.yral.shared.iap.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProductId(
    val productId: String,
) {
    PREMIUM_MONTHLY("premium_monthly"),
    PREMIUM_YEARLY("premium_yearly"),
    REMOVE_ADS("remove_ads"),
    ;

    companion object {
        fun fromString(productId: String): ProductId? = entries.find { it.productId == productId }
    }
}
