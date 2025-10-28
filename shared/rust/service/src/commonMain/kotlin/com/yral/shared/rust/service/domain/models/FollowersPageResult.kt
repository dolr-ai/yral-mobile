package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowersResponse

data class FollowersPageResult(
    val nextCursor: Principal?,
    val followers: List<FollowerItem>,
    val totalCount: ULong,
)

fun UisFollowersResponse.toFollowerPageResult(usernames: Map<String, String>): FollowersPageResult =
    FollowersPageResult(
        nextCursor = this.nextCursor,
        followers =
            this.followers.map { follower ->
                FollowerItem(
                    callerFollows = follower.callerFollows,
                    profilePictureUrl = follower.profilePictureUrl,
                    principalId = follower.principalId,
                    username = usernames[follower.principalId],
                )
            },
        totalCount = this.totalCount,
    )
