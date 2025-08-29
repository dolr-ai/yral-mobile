package com.yral.shared.features.feed.domain.models

import com.yral.shared.data.feed.domain.Post

data class PostResponse(
    val posts: List<Post>,
)
