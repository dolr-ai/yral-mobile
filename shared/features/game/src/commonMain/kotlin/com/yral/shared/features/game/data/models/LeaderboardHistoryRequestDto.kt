package com.yral.shared.features.game.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardHistoryRequestDto(
    @SerialName("principal_id")
    val principalId: String,
)
