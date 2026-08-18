package com.yral.shared.rust.service.domain.models

data class FollowersPageResult(
    val nextCursor: String?,
    val followers: List<FollowerItem>,
    val totalCount: ULong,
)
