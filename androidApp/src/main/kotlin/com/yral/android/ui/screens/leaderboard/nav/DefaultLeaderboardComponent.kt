package com.yral.android.ui.screens.leaderboard.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultLeaderboardComponent(
    componentContext: ComponentContext,
) : LeaderboardComponent,
    ComponentContext by componentContext,
    KoinComponent
