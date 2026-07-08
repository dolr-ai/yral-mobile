package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestImagesRequestDto(
    // Backend picks the URL set from this (clear vs pre-blurred); without it
    // it falls back to a YRAL-team-only stub, so real clients always send it.
    @SerialName("is_subscribed")
    val isSubscribed: Boolean,
)

/**
 * Shared response shape of POST /influencers/{id}/request-images and
 * GET /influencers/{id}/collage. `images` carries clear URLs for
 * subscribers and pre-blurred ones otherwise — the client never blurs.
 */
@Serializable
data class CollageResponseDto(
    @SerialName("images")
    val images: List<String> = emptyList(),
    @SerialName("is_blurred")
    val isBlurred: Boolean = false,
    @SerialName("theme")
    val theme: String? = null,
    @SerialName("generated_at")
    val generatedAt: String? = null,
    @SerialName("collage_bot_id")
    val collageBotId: String,
    @SerialName("collage_date")
    val collageDate: String,
)
