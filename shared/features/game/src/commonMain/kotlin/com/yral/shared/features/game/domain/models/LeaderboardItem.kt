package com.yral.shared.features.game.domain.models

import com.yral.shared.firebaseStore.model.LeaderboardItemDto

data class LeaderboardItem(
    val userPrincipalId: String,
    val coins: Long,
)

fun LeaderboardItemDto.toLeaderboardItem(): LeaderboardItem =
    LeaderboardItem(
        userPrincipalId = id,
        coins = coins,
    )
