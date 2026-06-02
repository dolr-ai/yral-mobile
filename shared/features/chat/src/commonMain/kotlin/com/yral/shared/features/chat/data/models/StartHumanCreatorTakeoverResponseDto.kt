package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StartHumanCreatorTakeoverResponseDto(
    @SerialName("status")
    val status: String,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("user_last_message_at")
    val userLastMessageAt: String? = null,
    @SerialName("remaining_seconds")
    val remainingSeconds: Int,
)
