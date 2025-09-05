package com.yral.android.ui.screens.leaderboard.details

import com.arkivanov.decompose.ComponentContext

abstract class LeaderboardDetailsComponent {
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit,
        ): LeaderboardDetailsComponent =
            DefaultLeaderboardDetailsComponent(
                componentContext = componentContext,
                onBack = onBack,
            )
    }
}
