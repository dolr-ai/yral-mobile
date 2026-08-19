package com.yral.shared.rust.service.domain.models

data class FollowerItem(
    val callerFollows: Boolean,
    val profilePictureUrl: String?,
    val principalId: String,
    val username: String?,
)
