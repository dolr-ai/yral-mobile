package com.yral.shared.features.wallet.data.models

import com.yral.shared.features.wallet.domain.models.DolrPrice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DolrPriceResponseDto(
    @SerialName("dolr-ai")
    val dolrAi: DolrPriceDataDto,
)

@Serializable
data class DolrPriceDataDto(
    val usd: Double,
    val inr: Double,
)

fun DolrPriceResponseDto.toDomain(): DolrPrice =
    DolrPrice(
        usdPrice = dolrAi.usd,
        inrPrice = dolrAi.inr,
    )
