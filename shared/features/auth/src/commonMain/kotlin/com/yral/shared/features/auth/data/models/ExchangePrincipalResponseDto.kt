package com.yral.shared.features.auth.data.models

import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import kotlinx.serialization.Serializable

@Serializable
data class ExchangePrincipalResponseDto(
    val coins: Long,
    val token: String,
)

fun ExchangePrincipalResponseDto.toExchangePrincipalResponse(): ExchangePrincipalResponse =
    ExchangePrincipalResponse(
        coins = coins,
        token = token,
    )
