package com.yral.shared.rust.service.domain.models

import com.yral.shared.data.domain.models.FeedDetails

sealed class Posts {
    data class Ok(
        val v1: List<FeedDetails?>,
    ) : Posts()
    data class Err(
        val v1: PostsOfUserProfileError,
    ) : Posts()
}

enum class PostsOfUserProfileError {
    REACHED_END_OF_ITEMS_LIST,
    INVALID_BOUNDS_PASSED,
    EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST,
}
