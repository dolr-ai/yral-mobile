package com.yral.shared.rust.service.domain.models

import com.yral.shared.rust.service.utils.toPrincipalText
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowingResponse

data class FollowingPageResult(
    val nextCursor: Principal?,
    val following: List<FollowerItem>,
    val totalCount: ULong,
)

fun UisFollowingResponse.toFollowingPageResult(usernames: Map<String, String>): FollowingPageResult =
    FollowingPageResult(
        nextCursor = this.nextCursor,
        following =
            this.following.map { follower ->
                val principalText = follower.principalId.toPrincipalText()
                FollowerItem(
                    callerFollows = follower.callerFollows,
                    profilePictureUrl = follower.profilePictureUrl,
                    principalId = follower.principalId,
                    username = usernames[principalText],
                )
            },
        totalCount = this.totalCount,
    )
