package com.yral.android.ui.screens.leaderboard

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.home.nav.HomeChildSnapshotProvider
import com.yral.android.ui.screens.leaderboard.details.LeaderboardDetailsComponent
import com.yral.android.ui.screens.leaderboard.main.LeaderboardMainComponent
import kotlinx.serialization.Serializable

abstract class LeaderboardComponent : HomeChildSnapshotProvider {
    abstract val stack: Value<ChildStack<*, Child>>

    abstract fun onBackClicked(): Boolean

    sealed class Child {
        class Main(
            val component: LeaderboardMainComponent,
        ) : Child()
        class Details(
            val component: LeaderboardDetailsComponent,
        ) : Child()
    }

    @Serializable
    data class Snapshot(
        val routes: List<Route>,
    ) {
        @Serializable
        enum class Route { Main, Details }
    }

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            snapshot: Snapshot?,
        ): LeaderboardComponent =
            DefaultLeaderboardComponent(
                componentContext = componentContext,
                snapshot = snapshot,
            )
    }
}
