package com.yral.shared.features.leaderboard.nav.main

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.rust.service.utils.CanisterData

interface LeaderboardMainComponent {
    fun openDailyHistory()
    fun navigateToHome()
    fun openProfile(userCanisterData: CanisterData)
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onDailyHistoryClicked: () -> Unit,
            navigateToHome: () -> Unit,
            openProfile: (CanisterData) -> Unit,
        ): LeaderboardMainComponent =
            DefaultLeaderboardMainComponent(
                componentContext = componentContext,
                onDailyHistoryClicked = onDailyHistoryClicked,
                navigateToHome = navigateToHome,
                openProfile = openProfile,
            )
    }
}
