package com.yral.shared.app.nav

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.app.UpdateState
import com.yral.shared.app.ui.screens.alertsrequest.nav.AlertsRequestComponent
import com.yral.shared.app.ui.screens.home.nav.HomeComponent
import com.yral.shared.features.auth.nav.countryselector.CountrySelectorComponent
import com.yral.shared.features.auth.nav.mandatorylogin.MandatoryLoginComponent
import com.yral.shared.features.auth.nav.otpverification.OtpVerificationComponent
import com.yral.shared.features.auth.ui.LoginCoordinator
import com.yral.shared.features.auth.ui.LoginInfo
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.features.root.viewmodels.RootViewModel
import com.yral.shared.features.subscriptions.nav.SubscriptionsComponent
import com.yral.shared.features.subscriptions.ui.SubscriptionCoordinator
import com.yral.shared.features.tournament.nav.TournamentGameComponent
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.rust.service.utils.CanisterData

@Suppress("TooManyFunctions")
interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    val slot: Value<ChildSlot<*, SlotChild>>
    val updateState: Value<UpdateState>
    val loginViewModel: LoginViewModel
    val rootViewModel: RootViewModel
    var currentLoginInfo: LoginInfo?

    fun onBackClicked()

    fun isSplashActive(): Boolean

    fun isMandatoryLoginActive(): Boolean

    fun navigateToSplash()

    fun navigateToMandatoryLogin()

    fun navigateToHome()

    fun onNavigationRequest(appRoute: AppRoute)

    fun onUpdateStateChanged(state: UpdateState)

    fun onCompleteUpdateClicked()

    fun openEditProfile()

    fun openProfile(userCanisterData: CanisterData)

    fun openTournamentLeaderboard(
        tournamentId: String,
        showResult: Boolean = false,
    )

    fun openTournamentResults(
        tournamentId: String,
        showResult: Boolean = false,
    )

    fun openTournamentGame(
        tournamentId: String,
        tournamentTitle: String,
        initialDiamonds: Int,
        startEpochMs: Long,
        endEpochMs: Long,
        totalPrizePool: Int,
        isHotOrNot: Boolean = false,
    )

    fun openConversation(
        influencerId: String,
        influencerCategory: String = "",
        influencerSource: InfluencerSource = InfluencerSource.CARD,
    )

    fun openWallet()

    fun openLeaderboard()

    fun openSubscription(validTill: Long?)

    fun onCreateVideo()

    fun onExploreFeed()

    fun getLoginCoordinator(): LoginCoordinator

    fun getSubscriptionCoordinator(): SubscriptionCoordinator

    fun createLoginRequestFactory(): RequestLoginFactory

    fun clearLoginState()

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
            val showResult: Boolean,
        ) : Child()
        class TournamentGame(
            val component: TournamentGameComponent,
        ) : Child()
        class Conversation(
            val component: ConversationComponent,
        ) : Child()
        class Wallet(
            val component: WalletComponent,
        ) : Child()
        class Leaderboard(
            val component: LeaderboardComponent,
        ) : Child()
        class Subscription(
            val component: SubscriptionsComponent,
        ) : Child()
        class CountrySelector(
            val component: CountrySelectorComponent,
        ) : Child()
        class OtpVerification(
            val component: OtpVerificationComponent,
        ) : Child()
        class MandatoryLogin(
            val component: MandatoryLoginComponent,
        ) : Child()
    }

    sealed class SlotChild {
        class AlertsRequestBottomSheet(
            val component: AlertsRequestComponent,
        ) : SlotChild()

        class LoginBottomSheet : SlotChild()

        class SubscriptionAccountMismatchSheet : SlotChild()
    }
}
