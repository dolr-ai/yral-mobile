package com.yral.android.ui.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.home.nav.HomeComponent
import kotlinx.serialization.Serializable

internal class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent,
    ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::child,
        )

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): RootComponent.Child =
        when (config) {
            is Config.Splash -> RootComponent.Child.Splash(splashComponent(componentContext))
            is Config.Home -> RootComponent.Child.Home(homeComponent(componentContext))
        }

    @Suppress("MaxLineLength")
    private fun splashComponent(componentContext: ComponentContext): SplashComponent = SplashComponent(componentContext = componentContext)

    private fun homeComponent(componentContext: ComponentContext): HomeComponent =
        HomeComponent.Companion(componentContext = componentContext)

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun setSplashActive(active: Boolean) {
        if (active == isSplashActive()) return
        val config = if (active) Config.Splash else Config.Home
        navigation.replaceAll(config)
    }

    private fun isSplashActive(): Boolean = stack.active.instance is RootComponent.Child.Splash

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Splash : Config

        @Serializable
        data object Home : Config
    }
}
