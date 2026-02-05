package com.yral.shared.features.leaderboard.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.leaderboard.domain.models.DailyRankGameType
import com.yral.shared.features.leaderboard.nav.detail.LeaderboardDetailsComponent
import com.yral.shared.features.leaderboard.nav.main.LeaderboardMainComponent
import com.yral.shared.libs.arch.nav.HomeChildSnapshotProvider
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.serialization.Serializable

abstract class LeaderboardComponent : HomeChildSnapshotProvider {
    abstract val stack: Value<ChildStack<*, Child>>

    abstract fun onBackClicked(): Boolean

    abstract val showBackIcon: Boolean
    abstract val onBack: () -> Unit
    abstract val gameType: DailyRankGameType

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
            navigateToHome: () -> Unit,
            openProfile: (CanisterData) -> Unit,
            showBackIcon: Boolean = false,
            onBack: () -> Unit = {},
            gameType: DailyRankGameType = DailyRankGameType.SMILEY,
        ): LeaderboardComponent =
            DefaultLeaderboardComponent(
                componentContext = componentContext,
                snapshot = snapshot,
                navigateToHome = navigateToHome,
                openProfile = openProfile,
                showBackIcon = showBackIcon,
                onBack = onBack,
                gameType = gameType,
            )
    }
}
