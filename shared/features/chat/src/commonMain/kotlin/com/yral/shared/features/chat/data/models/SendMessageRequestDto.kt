package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequestDto(
    @SerialName("content")
    val content: String? = null,
    @SerialName("message_type")
    val messageType: String,
    @SerialName("media_urls")
    val mediaUrls: List<String>? = null,
    @SerialName("audio_url")
    val audioUrl: String? = null,
    @SerialName("audio_duration_seconds")
    val audioDurationSeconds: Int? = null,
    // When true, asks the peer for a paid image that arrives blurred until
    // unlocked. Nullable so it's omitted from the JSON for ordinary sends.
    @SerialName("is_blur")
    val isBlur: Boolean? = null,
)
