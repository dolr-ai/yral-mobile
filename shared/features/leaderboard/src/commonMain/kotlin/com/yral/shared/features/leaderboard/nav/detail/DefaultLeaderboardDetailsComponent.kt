package com.yral.shared.features.leaderboard.nav.detail

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.features.leaderboard.domain.models.DailyRankGameType
import com.yral.shared.rust.service.utils.CanisterData
import org.koin.core.component.KoinComponent

internal class DefaultLeaderboardDetailsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    private val openProfile: (CanisterData) -> Unit,
    override val gameType: DailyRankGameType = DailyRankGameType.SMILEY,
) : LeaderboardDetailsComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }

    override fun openProfile(userCanisterData: CanisterData) {
        openProfile.invoke(userCanisterData)
    }
}
