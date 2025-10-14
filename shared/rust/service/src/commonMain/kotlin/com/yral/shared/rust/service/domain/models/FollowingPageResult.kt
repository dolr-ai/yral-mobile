package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowingResponse

data class FollowingPageResult(
    val nextCursor: Principal?,
    val following: List<FollowerItem>,
    val totalCount: ULong,
)

fun UisFollowingResponse.toFollowingPageResult(): FollowingPageResult =
    FollowingPageResult(
        nextCursor = this.nextCursor,
        following =
            this.following.map { follower ->
                FollowerItem(
                    callerFollows = follower.callerFollows,
                    profilePictureUrl = follower.profilePictureUrl,
                    principalId = follower.principalId,
                )
            },
        totalCount = this.totalCount,
    )
