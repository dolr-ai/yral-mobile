package com.yral.shared.features.wallet.data.models

import com.yral.shared.features.wallet.domain.models.BtcInInr
import kotlinx.serialization.Serializable

@Serializable
data class BtcPriceResponseDto(
    val inr: Double,
)

fun BtcPriceResponseDto.toBtcInInr(): BtcInInr = BtcInInr(priceInInr = this.inr)
