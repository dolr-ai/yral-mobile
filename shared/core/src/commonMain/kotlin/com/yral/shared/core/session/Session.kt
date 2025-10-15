package com.yral.shared.core.session

data class Session(
    val identity: ByteArray? = null,
    val canisterId: String? = null,
    val userPrincipal: String? = null,
    val profilePic: String? = null,
    val username: String? = null,
    val isCreatedFromServiceCanister: Boolean = false,
)

data class SessionProperties(
    val coinBalance: Long? = null,
    val isSocialSignIn: Boolean? = null,
    val profileVideosCount: Int? = null,
    val isForcedGamePlayUser: Boolean? = null,
    val emailId: String? = null,
    val isFirebaseLoggedIn: Boolean = false,
    val dailyRank: Long? = null,
)
