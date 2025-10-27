package com.yral.shared.features.feed.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AIFeedRequestDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("count")
    val count: Int,
    @SerialName("rec_type")
    val recommendationType: String,
)
