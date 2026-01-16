package com.yral.shared.rust.service.data.models

import com.yral.shared.rust.service.domain.models.Posts
import com.yral.shared.rust.service.domain.models.PostsOfUserProfileError
import com.yral.shared.rust.service.domain.models.toFeedDetails
import com.yral.shared.uniffi.generated.GetPostsOfUserProfileError
import com.yral.shared.uniffi.generated.Result12
import com.yral.shared.uniffi.generated.UpsGetPostsOfUserProfileError
import com.yral.shared.uniffi.generated.UpsResult3

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

internal fun UpsResult3.toPosts(canisterId: String): Posts =
    when (this) {
        is UpsResult3.Ok ->
            Posts.Ok(
                v1.map {
                    it.toFeedDetails(
                        postId = it.id,
                        canisterId = canisterId,
                        nsfwProbability = 0.0,
                    )
                },
            )
        is UpsResult3.Err ->
            // UpsResult3.Err carries a string; fallback to a generic error mapping
            Posts.Err(PostsOfUserProfileError.INVALID_BOUNDS_PASSED)
    }

fun UpsGetPostsOfUserProfileError.toPostsOfUserProfileError(): PostsOfUserProfileError =
    when (this) {
        UpsGetPostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST -> PostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST
        UpsGetPostsOfUserProfileError.INVALID_BOUNDS_PASSED -> PostsOfUserProfileError.INVALID_BOUNDS_PASSED
        UpsGetPostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST ->
            PostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST
    }
