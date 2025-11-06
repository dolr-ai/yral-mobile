package com.yral.shared.features.leaderboard.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.leaderboard.nav.detail.LeaderboardDetailsComponent
import com.yral.shared.features.leaderboard.nav.main.LeaderboardMainComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

internal class DefaultLeaderboardComponent(
    componentContext: ComponentContext,
    private val snapshot: Snapshot?,
    private val navigateToHome: () -> Unit,
) : LeaderboardComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialStack = {
                val saved = snapshot?.routes ?: emptyList()
                if (saved.isEmpty()) listOf(Config.Main) else saved.map { it.toConfig() }
            },
            handleBackButton = true,
            childFactory = ::child,
        )

    override fun onBackClicked(): Boolean {
        val items = stack.value.items
        return if (items.size > 1) {
            navigation.pop()
            true
        } else {
            false
        }
    }

    override fun createHomeSnapshot(): Snapshot =
        Snapshot(
            routes =
                stack.value.items.map { item ->
                    when (item.configuration) {
                        is Config.Main -> Snapshot.Route.Main
                        is Config.Details -> Snapshot.Route.Details
                        else -> Snapshot.Route.Main
                    }
                },
        )

    private fun Snapshot.Route.toConfig(): Config =
        when (this) {
            Snapshot.Route.Main -> Config.Main
            Snapshot.Route.Details -> Config.Details
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.Main -> Child.Main(leaderboardMainComponent(componentContext))
            Config.Details -> Child.Details(detailsComponent(componentContext))
        }

    private fun leaderboardMainComponent(componentContext: ComponentContext): LeaderboardMainComponent =
        LeaderboardMainComponent.Companion(
            componentContext = componentContext,
            onDailyHistoryClicked = { navigation.pushNew(Config.Details) },
            navigateToHome = navigateToHome,
        )

    private fun detailsComponent(componentContext: ComponentContext): LeaderboardDetailsComponent =
        LeaderboardDetailsComponent.Companion(
            componentContext = componentContext,
            onBack = { navigation.pop() },
        )

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Main : Config

        @Serializable
        data object Details : Config
    }
}
