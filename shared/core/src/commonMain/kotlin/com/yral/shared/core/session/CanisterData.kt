package com.yral.shared.core.session

import kotlinx.serialization.Serializable

@Serializable
data class CanisterData(
    val canisterId: String,
    val userPrincipalId: String,
    val profilePic: String,
    val username: String?,
    val isCreatedFromServiceCanister: Boolean,
    val isFollowing: Boolean = false,
)
