package com.yral.shared.rust.service.domain.models

import com.yral.shared.rust.service.utils.toPrincipalText
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
