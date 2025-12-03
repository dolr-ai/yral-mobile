package com.yral.shared.iap.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductId(
    val productId: String,
) {
    @SerialName("premium_monthly")
    PREMIUM_MONTHLY("premium_monthly"),

    @SerialName("premium_yearly")
    PREMIUM_YEARLY("premium_yearly"),

    @SerialName("remove_ads")
    REMOVE_ADS("remove_ads"),
    ;

    companion object {
        fun fromString(productId: String): ProductId? = entries.find { it.productId == productId }
    }
}
