package com.yral.shared.app.ui.screens.profile.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.account.nav.AccountComponent
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.libs.arch.nav.HomeChildSnapshotProvider
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

abstract class ProfileComponent : HomeChildSnapshotProvider {
    abstract val stack: Value<ChildStack<*, Child>>

    abstract val pendingVideoNavigation: Flow<String?>
    abstract val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    abstract val promptLogin: () -> Unit
    abstract fun onUploadVideoClick()
    abstract fun onNavigationRequest(appRoute: AppRoute)
    abstract fun openAccount()
    abstract fun openEditProfile()
    abstract fun onBackClicked(): Boolean
    abstract fun openProfile()

    sealed class Child {
        class Main(
            val component: ProfileMainComponent,
        ) : Child()
        class Account(
            val component: AccountComponent,
        ) : Child()
        class EditProfile(
            val component: EditProfileComponent,
        ) : Child()
    }

    @Serializable
    data class Snapshot(
        val routes: List<Route>,
    ) {
        @Serializable
        enum class Route { Main, Account, EditProfile }
    }

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            snapshot: Snapshot?,
            onUploadVideoClicked: () -> Unit,
            openEditProfile: () -> Unit,
            openProfile: (CanisterData) -> Unit,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
            promptLogin: () -> Unit,
        ): ProfileComponent =
            DefaultProfileComponent(
                componentContext,
                snapshot,
                onUploadVideoClicked,
                openEditProfile,
                openProfile,
                showAlertsOnDialog,
                promptLogin,
            )
    }
}
