package com.yral.shared.rust.service.domain.models

data class FollowingPageResult(
    val nextCursor: String?,
    val following: List<FollowerItem>,
    val totalCount: ULong,
)
