package com.yral.shared.features.leaderboard.nav.detail

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.features.leaderboard.domain.models.DailyRankGameType
import com.yral.shared.rust.service.utils.CanisterData

abstract class LeaderboardDetailsComponent {
    abstract val gameType: DailyRankGameType
    abstract fun onBack()
    abstract fun openProfile(userCanisterData: CanisterData)

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit,
            openProfile: (CanisterData) -> Unit,
            gameType: DailyRankGameType = DailyRankGameType.SMILEY,
        ): LeaderboardDetailsComponent =
            DefaultLeaderboardDetailsComponent(
                componentContext = componentContext,
                onBack = onBack,
                openProfile = openProfile,
                gameType = gameType,
            )
    }
}
