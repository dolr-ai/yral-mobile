package com.yral.shared.core.session

data class AccountInfo(
    val userPrincipal: String,
    val profilePic: String,
    val username: String? = null,
) {
    val displayName: String
        get() = username?.takeIf { it.isNotBlank() } ?: userPrincipal
}
