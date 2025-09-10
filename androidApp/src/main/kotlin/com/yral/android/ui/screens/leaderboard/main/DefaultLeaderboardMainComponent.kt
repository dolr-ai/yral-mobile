package com.yral.android.ui.screens.leaderboard.main

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultLeaderboardMainComponent(
    componentContext: ComponentContext,
    private val onDailyHistoryClicked: () -> Unit,
) : LeaderboardMainComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun openDailyHistory() {
        onDailyHistoryClicked.invoke()
    }
}
