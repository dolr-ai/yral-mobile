package com.yral.shared.features.leaderboard.nav.detail

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultLeaderboardDetailsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : LeaderboardDetailsComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }
}
