package com.yral.shared.features.profile.data.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FollowNotificationDto(
    @SerialName("delegated_identity_wire")
    val delegatedIdentity: KotlinDelegatedIdentityWire,
    @SerialName("follower_username")
    val followerUsername: String,
    @SerialName("target_principal")
    val targetPrincipal: String,
)
