package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for `POST /api/v1/chat/human/conversations`.
 *
 * The endpoint is idempotent — if an H2H conversation between the
 * authenticated user and [participantId] already exists, the backend
 * returns the existing one rather than creating a duplicate. So callers
 * can treat both 200 and 201 responses as success.
 */
@Serializable
data class CreateHumanConversationRequestDto(
    @SerialName("participant_id")
    val participantId: String,
)
