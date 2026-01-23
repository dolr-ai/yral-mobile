package com.yral.shared.iap.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductId(
    val productId: String,
) {
    @SerialName("yral_pro")
    YRAL_PRO("yral_pro"),
    ;

    val productType: ProductType
        get() =
            when (this) {
                YRAL_PRO -> ProductType.SUBS
            }

    companion object {
        fun fromString(productId: String): ProductId? = entries.find { it.productId == productId }
    }
}
