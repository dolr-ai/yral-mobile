package com.yral.android.ui.nav

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.home.nav.HomeComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    fun isSplashActive(): Boolean

    fun setSplashActive(active: Boolean)

    fun handleNavigation(destination: String)

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
