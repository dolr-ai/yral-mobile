package com.yral.shared.features.leaderboard.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.leaderboard.nav.detail.LeaderboardDetailsComponent
import com.yral.shared.features.leaderboard.nav.main.LeaderboardMainComponent
import com.yral.shared.libs.arch.nav.HomeChildSnapshotProvider
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
