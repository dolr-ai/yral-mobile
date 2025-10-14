package com.yral.shared.rust.service.domain.models

data class PagedFollowerItem(
    val items: List<FollowerItem>,
    val totalCount: ULong,
)
