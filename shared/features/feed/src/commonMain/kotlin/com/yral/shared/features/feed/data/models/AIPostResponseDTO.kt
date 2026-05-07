package com.yral.shared.features.feed.data.models

import com.yral.shared.data.data.models.PostDTO
import com.yral.shared.features.feed.domain.models.PostResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AIPostResponseDTO(
    @SerialName("user_id")
    val userId: String,
    @SerialName("videos")
    val videos: List<PostDTO>,
    val count: Int,
    val sources: Map<String, Int> = emptyMap(),
    val timestamp: Long = 0L,
)

fun AIPostResponseDTO.toPostResponse(): PostResponse =
    PostResponse(
        posts =
            videos.map {
                it.toPost()
            },
    )
