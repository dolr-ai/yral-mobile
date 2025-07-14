package com.yral.shared.core.session

data class Session(
    val identity: ByteArray? = null,
    val canisterPrincipal: String? = null,
    val userPrincipal: String? = null,
)

data class SessionProperties(
    val coinBalance: Long,
    val isSocialSignIn: Boolean,
    val profileVideosCount: Int,
)
