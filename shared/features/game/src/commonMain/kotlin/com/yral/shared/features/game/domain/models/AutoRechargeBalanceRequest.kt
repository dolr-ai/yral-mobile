package com.yral.shared.features.game.domain.models

import com.yral.shared.features.game.data.models.AutoRechargeBalanceRequestDto

data class AutoRechargeBalanceRequest(
    val idToken: String = "",
    val principalId: String,
)

fun AutoRechargeBalanceRequest.toDto(): AutoRechargeBalanceRequestDto =
    AutoRechargeBalanceRequestDto(
        principalId = principalId,
    )
