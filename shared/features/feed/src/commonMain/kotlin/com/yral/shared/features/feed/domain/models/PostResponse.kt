package com.yral.shared.features.feed.domain.models

import com.yral.shared.data.domain.models.Post

data class PostResponse(
    val posts: List<Post>,
)
