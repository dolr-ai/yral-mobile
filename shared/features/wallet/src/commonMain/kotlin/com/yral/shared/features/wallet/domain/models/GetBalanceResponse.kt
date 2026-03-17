package com.yral.shared.features.wallet.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class GetBalanceResponse(
    val balance: Long,
)
