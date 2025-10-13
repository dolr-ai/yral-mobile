package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.Principal

data class FollowerItem(
    val callerFollows: Boolean,
    val profilePictureUrl: String?,
    val principalId: Principal,
)
