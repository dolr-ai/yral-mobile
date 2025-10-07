package com.yral.shared.features.leaderboard.nav.main

import com.arkivanov.decompose.ComponentContext

interface LeaderboardMainComponent {
    fun openDailyHistory()
    fun navigateToHome()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onDailyHistoryClicked: () -> Unit,
            navigateToHome: () -> Unit,
        ): LeaderboardMainComponent =
            DefaultLeaderboardMainComponent(
                componentContext = componentContext,
                onDailyHistoryClicked = onDailyHistoryClicked,
                navigateToHome = navigateToHome,
            )
    }
}
