package com.yral.shared.features.game.domain.models

data class CurrentUserInfo(
    val userPrincipalId: String,
    val profileImageUrl: String,
    val coins: Long,
    val leaderboardPosition: Int,
    val rank: Int,
)
