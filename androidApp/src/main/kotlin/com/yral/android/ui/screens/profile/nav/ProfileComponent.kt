package com.yral.android.ui.screens.profile.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.account.nav.AccountComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.libs.arch.nav.HomeChildSnapshotProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

abstract class ProfileComponent : HomeChildSnapshotProvider {
    abstract val stack: Value<ChildStack<*, Child>>

    abstract val pendingVideoNavigation: Flow<String?>
    abstract fun onUploadVideoClick()
    abstract fun handleNavigation(destination: String)
    abstract fun openAccount()
    abstract fun onBackClicked(): Boolean

    sealed class Child {
        class Main(
            val component: ProfileMainComponent,
        ) : Child()
        class Account(
            val component: AccountComponent,
        ) : Child()
    }

    @Serializable
    data class Snapshot(
        val routes: List<Route>,
    ) {
        @Serializable
        enum class Route { Main, Account }
    }

    companion object Companion {
        const val DEEPLINK = "yralm://profile"
        const val DEEPLINK_VIDEO_PREFIX = "$DEEPLINK/videos"
        operator fun invoke(
            componentContext: ComponentContext,
            snapshot: Snapshot?,
            onUploadVideoClicked: () -> Unit,
        ): ProfileComponent = DefaultProfileComponent(componentContext, snapshot, onUploadVideoClicked)
    }
}
