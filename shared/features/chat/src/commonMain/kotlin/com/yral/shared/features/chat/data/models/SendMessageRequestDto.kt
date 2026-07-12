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
    // Collage sends store only this reference — never image URLs — so the
    // bubble can refetch with the current subscription state forever after.
    // collage_id is the primary handle; bot_id + date stay for legacy/debug.
    // Nullable so all three are omitted from the JSON for ordinary sends.
    @SerialName("collage_id")
    val collageId: String? = null,
    @SerialName("collage_bot_id")
    val collageBotId: String? = null,
    @SerialName("collage_date")
    val collageDate: String? = null,
)
