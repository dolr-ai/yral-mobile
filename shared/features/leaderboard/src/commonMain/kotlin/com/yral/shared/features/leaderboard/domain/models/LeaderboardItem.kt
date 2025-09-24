package com.yral.shared.features.leaderboard.domain.models

import com.yral.shared.features.leaderboard.data.models.LeaderboardRowDto
import com.yral.shared.rust.service.utils.propicFromPrincipal

data class LeaderboardItem(
    val userPrincipalId: String,
    val profileImage: String,
    val wins: Long,
    val position: Int,
    val reward: Double?,
)

fun LeaderboardRowDto.toLeaderboardItem(): LeaderboardItem =
    LeaderboardItem(
        userPrincipalId = principalId,
        profileImage = propicFromPrincipal(principalId),
        wins = wins,
        position = position,
        reward = reward,
    )
