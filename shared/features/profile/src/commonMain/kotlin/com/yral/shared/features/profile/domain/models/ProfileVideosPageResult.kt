package com.yral.shared.features.profile.domain.models

import com.yral.shared.data.domain.models.FeedDetails

data class ProfileVideosPageResult(
    val posts: List<FeedDetails>,
    val hasNextPage: Boolean,
    val nextStartIndex: ULong,
)
