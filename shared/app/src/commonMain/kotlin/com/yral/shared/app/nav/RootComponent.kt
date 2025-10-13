package com.yral.shared.app.nav

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.app.UpdateState
import com.yral.shared.app.ui.screens.home.nav.HomeComponent
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.libs.routing.routes.api.AppRoute

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    val updateState: Value<UpdateState>

    fun onBackClicked()

    fun isSplashActive(): Boolean

    fun setSplashActive(active: Boolean)

    fun onNavigationRequest(appRoute: AppRoute)

    fun onUpdateStateChanged(state: UpdateState)

    fun onCompleteUpdateClicked()

    fun openEditProfile()

    // Defines all possible child components
    sealed class Child {
        class Splash(
            val component: SplashComponent,
        ) : Child()
        class Home(
            val component: HomeComponent,
        ) : Child()
        class EditProfile(
            val component: EditProfileComponent,
        ) : Child()
    }
}
