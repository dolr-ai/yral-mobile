package com.yral.shared.features.subscriptions.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.subscriptions.nav.main.SubscriptionsMainComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

internal class DefaultSubscriptionsComponent(
    componentContext: ComponentContext,
    private val snapshot: SubscriptionsComponent.Snapshot?,
    override val showBackIcon: Boolean,
    override val onBack: () -> Unit,
) : SubscriptionsComponent(),
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

    override fun createHomeSnapshot(): SubscriptionsComponent.Snapshot =
        SubscriptionsComponent.Snapshot(
            routes =
                stack.value.items.map { item ->
                    when (item.configuration as Config) {
                        Config.Main -> SubscriptionsComponent.Snapshot.Route.Main
                    }
                },
        )

    private fun SubscriptionsComponent.Snapshot.Route.toConfig(): Config =
        when (this) {
            SubscriptionsComponent.Snapshot.Route.Main -> Config.Main
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.Main -> Child.Main(subscriptionsMainComponent(componentContext))
        }

    private fun subscriptionsMainComponent(componentContext: ComponentContext): SubscriptionsMainComponent =
        SubscriptionsMainComponent.Companion(
            componentContext = componentContext,
            showBackIcon = showBackIcon,
            onBack = onBack,
        )

    @Serializable
    private enum class Config {
        Main,
    }
}
