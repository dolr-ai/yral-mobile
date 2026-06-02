package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamDonePayloadDto(
    @SerialName("assistant_message")
    val assistantMessage: ChatMessageDto,
    @SerialName("provider")
    val provider: String,
    @SerialName("model")
    val model: String? = null,
    @SerialName("tokens")
    val tokens: Int = 0,
    @SerialName("blocked")
    val blocked: Boolean = false,
)
