package com.yral.shared.features.wallet.data.models

import com.yral.shared.features.wallet.domain.models.BtcToCurrency
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BtcPriceResponseDto(
    @SerialName("conversion_rate")
    val conversionRate: Double,
    @SerialName("currency_code")
    val currencyCode: String,
)

fun BtcPriceResponseDto.toDomain(): BtcToCurrency =
    BtcToCurrency(
        conversionRate = conversionRate,
        currencyCode = currencyCode,
    )
