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

    // Per-bot auto-renewing chat subscription (Tara). The billing backend
    // routes any "bot_sub"-prefixed product to its subscription flow; the
    // intro offer (3 days ₹9 → ₹69/week) is configured in the stores.
    @SerialName("bot_sub_tara")
    BOT_SUB_TARA("bot_sub_tara"),
    ;

    val productType: ProductType
        get() =
            when (this) {
                YRAL_PRO, BOT_SUB_TARA -> ProductType.SUBS
                DAILY_CHAT -> ProductType.ONE_TIME
            }

    companion object {
        fun fromString(productId: String): ProductId? = entries.find { it.productId == productId }
    }
}
