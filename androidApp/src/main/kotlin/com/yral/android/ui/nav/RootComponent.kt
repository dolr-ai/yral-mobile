package com.yral.android.ui.nav

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.home.nav.HomeComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    fun setSplashActive(active: Boolean)

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
