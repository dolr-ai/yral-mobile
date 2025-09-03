package com.yral.shared.features.profile.domain.models

import com.yral.shared.data.feed.domain.FeedDetails

data class ProfileVideosPageResult(
    val posts: List<FeedDetails>,
    val hasNextPage: Boolean,
    val nextStartIndex: ULong,
)
