package com.yral.shared.features.profile.domain.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.profile.data.models.FollowNotificationDto

data class FollowNotification(
    val delegatedIdentity: KotlinDelegatedIdentityWire,
    val followerUsername: String,
    val targetPrincipal: String,
)

fun FollowNotification.toDto(): FollowNotificationDto =
    FollowNotificationDto(
        delegatedIdentity = delegatedIdentity,
        followerUsername = followerUsername,
        targetPrincipal = targetPrincipal,
    )
