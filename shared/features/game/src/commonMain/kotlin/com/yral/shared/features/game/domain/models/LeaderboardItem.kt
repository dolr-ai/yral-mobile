package com.yral.shared.features.game.domain.models

import com.yral.shared.firebaseStore.model.LeaderboardItemDto
import com.yral.shared.uniffi.generated.propicFromPrincipal

data class LeaderboardItem(
    val userPrincipalId: String,
    val profileImage: String,
    val coins: Long,
)

fun LeaderboardItemDto.toLeaderboardItem(): LeaderboardItem =
    LeaderboardItem(
        userPrincipalId = id,
        profileImage = propicFromPrincipal(id),
        coins = coins,
    )
