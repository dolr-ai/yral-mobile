package com.yral.android.ui.nav

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.home.nav.HomeComponent
import com.yral.android.update.UpdateState
import com.yral.shared.libs.routing.routes.api.AppRoute

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    val updateState: Value<UpdateState>

    fun onBackClicked()

    fun isSplashActive(): Boolean

    fun setSplashActive(active: Boolean)

    @Deprecated("use onNavigationRequest")
    fun handleNavigation(destination: String)

    fun onNavigationRequest(appRoute: AppRoute)

    fun onUpdateStateChanged(state: UpdateState)

    fun onCompleteUpdateClicked()

    // Defines all possible child components
    sealed class Child {
        class Splash(
            val component: SplashComponent,
        ) : Child()
        class Home(
            val component: HomeComponent,
        ) : Child()
    }
}
