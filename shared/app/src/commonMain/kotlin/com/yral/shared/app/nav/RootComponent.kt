package com.yral.shared.app.nav

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.app.UpdateState
import com.yral.shared.app.ui.screens.alertsrequest.nav.AlertsRequestComponent
import com.yral.shared.app.ui.screens.home.nav.HomeComponent
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.features.tournament.nav.TournamentGameComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.rust.service.utils.CanisterData

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    val slot: Value<ChildSlot<*, SlotChild>>
    val updateState: Value<UpdateState>

    fun onBackClicked()

    fun isSplashActive(): Boolean

    fun setSplashActive(active: Boolean)

    fun onNavigationRequest(appRoute: AppRoute)

    fun onUpdateStateChanged(state: UpdateState)

    fun onCompleteUpdateClicked()

    fun openEditProfile()

    fun openProfile(userCanisterData: CanisterData)

    fun openTournamentLeaderboard(
        tournamentId: String,
        participantsLabel: String,
        scheduleLabel: String,
    )

    fun openTournamentGame(
        tournamentId: String,
        tournamentTitle: String,
        initialDiamonds: Int,
        endEpochMs: Long,
        totalPrizePool: Int,
    )

    fun showLoginBottomSheet(
        pageName: SignupPageName,
        loginBottomSheetType: LoginBottomSheetType,
        onDismissRequest: () -> Unit,
        onLoginSuccess: () -> Unit = {},
    )

    fun hideLoginBottomSheetIfVisible()

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
        class UserProfile(
            val component: ProfileMainComponent,
        ) : Child()
        class TournamentLeaderboard(
            val tournamentId: String,
            val participantsLabel: String,
            val scheduleLabel: String,
        ) : Child()
        class TournamentGame(
            val component: TournamentGameComponent,
        ) : Child()
    }

    sealed class SlotChild {
        class AlertsRequestBottomSheet(
            val component: AlertsRequestComponent,
        ) : SlotChild()

        class LoginBottomSheet(
            val pageName: SignupPageName,
            val loginBottomSheetType: LoginBottomSheetType,
            val onDismissRequest: () -> Unit,
            val onLoginSuccess: () -> Unit,
        ) : SlotChild()
    }
}
