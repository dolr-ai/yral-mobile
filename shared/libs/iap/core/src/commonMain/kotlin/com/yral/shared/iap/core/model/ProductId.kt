package com.yral.shared.iap.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductId(
    val productId: String,
) {
    @SerialName("yral_pro")
    YRAL_PRO("yral_pro"),

    @SerialName("tara_subscription")
    TARA_SUBSCRIPTION("tara_subscription"),
    ;

    val productType: ProductType
        get() =
            when (this) {
                YRAL_PRO -> ProductType.SUBS
                TARA_SUBSCRIPTION -> ProductType.SUBS
            }

    companion object {
        fun fromString(productId: String): ProductId? = entries.find { it.productId == productId }
    }
}
