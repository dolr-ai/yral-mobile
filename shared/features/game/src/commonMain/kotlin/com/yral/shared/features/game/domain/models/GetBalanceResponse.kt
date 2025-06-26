package com.yral.shared.features.game.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class GetBalanceResponse(
    val balance: Long,
)
