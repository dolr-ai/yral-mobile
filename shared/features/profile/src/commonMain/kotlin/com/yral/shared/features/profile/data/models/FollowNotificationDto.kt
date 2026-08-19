package com.yral.shared.features.profile.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FollowNotificationDto(
    @SerialName("follower_username")
    val followerUsername: String,
    @SerialName("target_principal")
    val targetPrincipal: String,
)
