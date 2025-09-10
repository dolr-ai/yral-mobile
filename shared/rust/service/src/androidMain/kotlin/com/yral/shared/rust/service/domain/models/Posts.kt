package com.yral.shared.rust.service.domain.models

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.uniffi.generated.GetPostsOfUserProfileError
import com.yral.shared.uniffi.generated.Result12
import com.yral.shared.uniffi.generated.ScGetPostsOfUserProfileError
import com.yral.shared.uniffi.generated.ScResult3

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

internal fun GetPostsOfUserProfileError.toPostsOfUserProfileError(): PostsOfUserProfileError =
    when (this) {
        GetPostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST -> PostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST
        GetPostsOfUserProfileError.INVALID_BOUNDS_PASSED -> PostsOfUserProfileError.INVALID_BOUNDS_PASSED
        GetPostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST ->
            PostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST
    }

internal fun Result12.toPosts(canisterId: String): Posts =
    when (this) {
        is Result12.Ok ->
            Posts.Ok(
                v1.map {
                    it.toFeedDetails(
                        postId = it.id.toString(),
                        canisterId = canisterId,
                        nsfwProbability = 0.0,
                    )
                },
            )

        is Result12.Err -> Posts.Err(v1.toPostsOfUserProfileError())
    }

internal fun ScResult3.toPosts(canisterId: String): Posts =
    when (this) {
        is ScResult3.Ok ->
            Posts.Ok(
                v1.map {
                    it.toFeedDetails(
                        postId = it.id,
                        canisterId = canisterId,
                        nsfwProbability = 0.0,
                    )
                },
            )
        is ScResult3.Err -> Posts.Err(v1.toPostsOfUserProfileError())
    }

fun ScGetPostsOfUserProfileError.toPostsOfUserProfileError(): PostsOfUserProfileError =
    when (this) {
        ScGetPostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST -> PostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST
        ScGetPostsOfUserProfileError.INVALID_BOUNDS_PASSED -> PostsOfUserProfileError.INVALID_BOUNDS_PASSED
        ScGetPostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST ->
            PostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST
    }
