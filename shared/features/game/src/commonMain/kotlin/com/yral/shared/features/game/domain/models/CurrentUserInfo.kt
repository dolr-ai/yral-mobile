package com.yral.shared.features.game.domain.models

data class CurrentUserInfo(
    val userPrincipalId: String,
    val profileImageUrl: String,
    val wins: Long,
    val leaderboardPosition: Int,
)
