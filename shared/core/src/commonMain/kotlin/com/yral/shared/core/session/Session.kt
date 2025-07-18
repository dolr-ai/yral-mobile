package com.yral.shared.core.session

data class Session(
    val identity: ByteArray? = null,
    val canisterId: String? = null,
    val userPrincipal: String? = null,
)

data class SessionProperties(
    val coinBalance: Long? = null,
    val isSocialSignIn: Boolean? = null,
    val profileVideosCount: Int? = null,
)
