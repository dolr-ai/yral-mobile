package com.yral.shared.core.session

data class Session(
    val identity: ByteArray? = null,
    val canisterId: String? = null,
    val userPrincipal: String? = null,
    val profilePic: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val isCreatedFromServiceCanister: Boolean = false,
)

data class SessionProperties(
    val coinBalance: Long? = null,
    val isSocialSignIn: Boolean? = null,
    val profileVideosCount: Int? = null,
    val isForcedGamePlayUser: Boolean? = null,
    val isAutoScrollEnabled: Boolean? = null,
    val emailId: String? = null,
    val isFirebaseLoggedIn: Boolean = false,
    val dailyRank: Long? = null,
    val followedPrincipals: Set<String> = setOf(),
    val unFollowedPrincipals: Set<String> = setOf(),
    val pendingTournamentRegistrationId: String? = null,
    val isMandatoryLogin: Boolean? = null,
)
