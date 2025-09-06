package com.yral.android.ui.screens.leaderboard.main

import com.arkivanov.decompose.ComponentContext

interface LeaderboardMainComponent {
    fun openDailyHistory()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onDailyHistoryClicked: () -> Unit,
        ): LeaderboardMainComponent =
            DefaultLeaderboardMainComponent(
                componentContext = componentContext,
                onDailyHistoryClicked = onDailyHistoryClicked,
            )
    }
}
