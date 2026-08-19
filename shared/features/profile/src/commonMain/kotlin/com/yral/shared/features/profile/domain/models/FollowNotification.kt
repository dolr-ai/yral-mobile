package com.yral.shared.features.profile.domain.models

import com.yral.shared.features.profile.data.models.FollowNotificationDto

data class FollowNotification(
    val followerUsername: String,
    val targetPrincipal: String,
)

fun FollowNotification.toDto(): FollowNotificationDto =
    FollowNotificationDto(
        followerUsername = followerUsername,
        targetPrincipal = targetPrincipal,
    )
