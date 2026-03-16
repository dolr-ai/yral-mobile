package com.yral.shared.iap.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductId(
    val productId: String,
) {
    @SerialName("yral_pro")
    YRAL_PRO("yral_pro"),

    @SerialName("daily_chat")
    DAILY_CHAT("daily_chat"),

    @Deprecated("Legacy subscription, use DAILY_CHAT instead")
    @SerialName("tara_subscription")
    TARA_SUBSCRIPTION("tara_subscription"),
    ;

    @Suppress("DEPRECATION")
    val productType: ProductType
        get() =
            when (this) {
                YRAL_PRO -> ProductType.SUBS
                DAILY_CHAT -> ProductType.ONE_TIME
                TARA_SUBSCRIPTION -> ProductType.SUBS
            }

    companion object {
        fun fromString(productId: String): ProductId? = entries.find { it.productId == productId }
    }
}
