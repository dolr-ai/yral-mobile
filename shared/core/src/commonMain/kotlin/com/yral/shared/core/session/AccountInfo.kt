package com.yral.shared.core.session

import com.yral.shared.core.utils.resolveUsername

data class AccountInfo(
    val userPrincipal: String,
    val profilePic: String,
    val username: String? = null,
) {
    val displayName: String
        get() = resolveUsername(username, userPrincipal) ?: userPrincipal
}
