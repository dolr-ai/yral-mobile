package com.yral.shared.features.profile.domain.models

data class DeleteVideoRequest(
    val postId: ULong,
    val videoId: String,
)
