package com.yral.shared.features.wallet.data.models

import com.yral.shared.features.wallet.domain.models.GetBalanceResponse
import kotlinx.serialization.Serializable

@Serializable
data class GetBalanceResponseDto(
    val balance: Long,
)

fun GetBalanceResponseDto.toGetBalanceResponse(): GetBalanceResponse =
    GetBalanceResponse(
        balance = balance,
    )
