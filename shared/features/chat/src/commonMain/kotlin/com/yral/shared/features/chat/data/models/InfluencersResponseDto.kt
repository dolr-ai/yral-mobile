package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InfluencersResponseDto(
    @SerialName("influencers")
    val influencers: List<InfluencerDto>,
    @SerialName("total")
    val total: Int,
    @SerialName("limit")
    val limit: Int,
    @SerialName("offset")
    val offset: Int,
    @SerialName("has_more")
    val hasMore: Boolean? = null,
)
