package com.yral.shared.features.game.domain.models

data class CurrentUserInfo(
    val userPrincipalId: String,
    val coins: Long,
    val leaderboardPosition: Int,
)
