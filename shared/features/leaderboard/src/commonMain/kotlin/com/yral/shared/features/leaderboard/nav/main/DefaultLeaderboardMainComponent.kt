package com.yral.shared.features.leaderboard.nav.main

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.rust.service.utils.CanisterData
import org.koin.core.component.KoinComponent

internal class DefaultLeaderboardMainComponent(
    componentContext: ComponentContext,
    private val onDailyHistoryClicked: () -> Unit,
    private val navigateToHome: () -> Unit,
    private val openProfile: (CanisterData) -> Unit,
    override val showBackIcon: Boolean,
    override val onBack: () -> Unit,
) : LeaderboardMainComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun openDailyHistory() {
        onDailyHistoryClicked.invoke()
    }

    override fun navigateToHome() {
        navigateToHome.invoke()
    }

    override fun openProfile(userCanisterData: CanisterData) {
        openProfile.invoke(userCanisterData)
    }
}
