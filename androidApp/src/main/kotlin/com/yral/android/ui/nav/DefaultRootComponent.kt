package com.yral.android.ui.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.home.nav.HomeComponent
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.android.update.UpdateState
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.BtcRewardsReceived
import kotlinx.serialization.Serializable

internal class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent,
    ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()
    private var homeComponent: HomeComponent? = null

    @Deprecated("Use pendingNavRoute")
    private var pendingNavigation: String? = null
    private var pendingNavRoute: AppRoute? = null

    private val _updateState = MutableValue<UpdateState>(UpdateState.Idle)
    override val updateState: Value<UpdateState> = _updateState

    private var onCompleteUpdateCallback: (() -> Unit)? = null

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::child,
        ).also { stackValue ->
            // Observe stack changes to handle pending navigation
            stackValue.subscribe { stack ->
                val navigation = pendingNavigation
                if (navigation != null && stack.active.instance is RootComponent.Child.Home) {
                    pendingNavigation = null
                    homeComponent?.handleNavigation(navigation)
                } else {
                    val navRoute = pendingNavRoute
                    if (navRoute != null && stack.active.instance is RootComponent.Child.Home) {
                        pendingNavRoute = null
                        homeComponent?.onNavigationRequest(navRoute)
                    }
                }
            }
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): RootComponent.Child =
        when (config) {
            is Config.Splash -> RootComponent.Child.Splash(splashComponent(componentContext))
            is Config.Home -> RootComponent.Child.Home(homeComponent(componentContext))
        }

    private fun splashComponent(componentContext: ComponentContext): SplashComponent =
        SplashComponent(
            componentContext = componentContext,
        )

    private fun homeComponent(componentContext: ComponentContext): HomeComponent {
        val component = HomeComponent.Companion(componentContext = componentContext)
        homeComponent = component
        return component
    }

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun setSplashActive(active: Boolean) {
        if (active == isSplashActive()) return
        val config = if (active) Config.Splash else Config.Home
        navigation.replaceAll(config)
    }

    override fun isSplashActive(): Boolean = stack.active.instance is RootComponent.Child.Splash

    @Deprecated("use onNavigationRequest")
    override fun handleNavigation(destination: String) {
        when {
            destination.startsWith(ProfileComponent.DEEPLINK) -> {
                if (isSplashActive()) {
                    pendingNavigation = destination
                } else {
                    homeComponent?.handleNavigation(destination)
                }
            }
            destination == BtcRewardsReceived.toString() -> {
                onNavigationRequest(BtcRewardsReceived)
            }
        }
    }

    override fun onNavigationRequest(appRoute: AppRoute) {
        if (isSplashActive()) {
            pendingNavRoute = appRoute
        } else {
            homeComponent?.onNavigationRequest(appRoute)
        }
    }

    override fun onUpdateStateChanged(state: UpdateState) {
        _updateState.value = state
    }

    override fun onCompleteUpdateClicked() {
        onCompleteUpdateCallback?.invoke()
    }

    fun setOnCompleteUpdateCallback(callback: () -> Unit) {
        onCompleteUpdateCallback = callback
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Splash : Config

        @Serializable
        data object Home : Config
    }
}
