package com.yral.shared.features.feed.data.models

import com.yral.shared.data.data.models.PostDTO
import com.yral.shared.features.feed.domain.models.PostResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GlobalCachePostResponseDTO(
    @SerialName("videos")
    val videos: List<PostDTO>,
    val count: Int? = null,
)

fun GlobalCachePostResponseDTO.toPostResponse(): PostResponse =
    PostResponse(
        posts =
            videos.map {
                it.toPost()
            },
    )
