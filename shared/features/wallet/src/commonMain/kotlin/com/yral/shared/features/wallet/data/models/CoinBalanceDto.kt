package com.yral.shared.features.wallet.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CoinBalanceDto(
    val balance: Long,
)
