package com.yral.android.ui.screens.leaderboard.nav

import com.arkivanov.decompose.ComponentContext

interface LeaderboardComponent {
    companion object Companion {
        operator fun invoke(componentContext: ComponentContext): LeaderboardComponent =
            DefaultLeaderboardComponent(
                componentContext,
            )
    }
}
