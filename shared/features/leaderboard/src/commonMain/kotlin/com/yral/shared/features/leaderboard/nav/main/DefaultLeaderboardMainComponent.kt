package com.yral.shared.features.leaderboard.nav.main

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultLeaderboardMainComponent(
    componentContext: ComponentContext,
    private val onDailyHistoryClicked: () -> Unit,
    private val navigateToHome: () -> Unit,
) : LeaderboardMainComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun openDailyHistory() {
        onDailyHistoryClicked.invoke()
    }

    override fun navigateToHome() {
        navigateToHome.invoke()
    }
}
