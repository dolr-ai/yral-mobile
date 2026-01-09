package com.yral.shared.app.nav.factories

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.app.nav.Config
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.nav.SplashComponent
import com.yral.shared.app.ui.screens.home.nav.HomeComponent
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.auth.nav.countryselector.CountrySelectorComponent
import com.yral.shared.features.auth.nav.mandatorylogin.MandatoryLoginComponent
import com.yral.shared.features.auth.nav.otpverification.OtpVerificationComponent
import com.yral.shared.features.auth.ui.LoginCoordinator
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.features.tournament.nav.TournamentGameComponent
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.libs.phonevalidation.countries.Country
import kotlinx.coroutines.flow.flowOf

/**
 * Factory for creating child components.
 * Keeps component creation logic separate from navigation.
 */
internal class ComponentFactory(
    private val rootComponent: RootComponent,
    private val loginCoordinator: LoginCoordinator,
    private val setHomeComponent: (HomeComponent) -> Unit,
    private val showAlertsOnDialog: (AlertsRequestType) -> Unit,
    private val onFeedTabClick: () -> Unit,
) {
    fun createSplash(componentContext: ComponentContext): SplashComponent =
        SplashComponent(
            componentContext = componentContext,
        )

    fun createHome(componentContext: ComponentContext): HomeComponent {
        val component =
            HomeComponent.Companion(
                componentContext = componentContext,
                requestLoginFactory = rootComponent.createLoginRequestFactory(),
                openEditProfile = rootComponent::openEditProfile,
                openProfile = rootComponent::openProfile,
                openTournamentLeaderboard = rootComponent::openTournamentLeaderboard,
                openTournamentGame = rootComponent::openTournamentGame,
                openConversation = rootComponent::openConversation,
                openWallet = rootComponent::openWallet,
                openLeaderboard = rootComponent::openLeaderboard,
                showAlertsOnDialog = showAlertsOnDialog,
            )
        setHomeComponent(component)
        return component
    }

    fun createEditProfile(componentContext: ComponentContext): EditProfileComponent =
        EditProfileComponent.Companion(
            componentContext = componentContext,
            onBack = rootComponent::onBackClicked,
        )

    fun createProfile(
        componentContext: ComponentContext,
        config: Config.UserProfile,
    ): ProfileMainComponent =
        ProfileMainComponent.invoke(
            componentContext = componentContext,
            requestLoginFactory = rootComponent.createLoginRequestFactory(),
            userCanisterData = config.userCanisterData,
            pendingVideoNavigation = flowOf(null),
            onUploadVideoClicked = {},
            openAccount = {},
            openEditProfile = {},
            openProfile = rootComponent::openProfile,
            openConversation = rootComponent::openConversation,
            onBackClicked = rootComponent::onBackClicked,
            showAlertsOnDialog = showAlertsOnDialog,
        )

    fun createConversation(
        componentContext: ComponentContext,
        config: Config.Conversation,
    ): ConversationComponent =
        ConversationComponent.Companion(
            componentContext = componentContext,
            requestLoginFactory = rootComponent.createLoginRequestFactory(),
            influencerId = config.influencerId,
            influencerCategory = config.influencerCategory,
            onBack = rootComponent::onBackClicked,
            openProfile = rootComponent::openProfile,
        )

    fun createWallet(componentContext: ComponentContext): WalletComponent =
        WalletComponent(
            componentContext = componentContext,
            showAlertsOnDialog = showAlertsOnDialog,
            showBackIcon = true,
            onBack = rootComponent::onBackClicked,
        )

    fun createLeaderboard(componentContext: ComponentContext): LeaderboardComponent =
        LeaderboardComponent.Companion(
            componentContext = componentContext,
            snapshot = null,
            navigateToHome = {
                rootComponent.onBackClicked()
                onFeedTabClick()
            },
            openProfile = rootComponent::openProfile,
            showBackIcon = true,
            onBack = rootComponent::onBackClicked,
        )

    fun createCountrySelector(
        componentContext: ComponentContext,
        onCountrySelected: (Country) -> Unit,
        onBack: () -> Unit,
    ): CountrySelectorComponent =
        CountrySelectorComponent.Companion(
            componentContext = componentContext,
            onCountrySelected = onCountrySelected,
            onBack = onBack,
        )

    fun createOtpVerification(
        componentContext: ComponentContext,
        onBack: () -> Unit,
    ): OtpVerificationComponent =
        OtpVerificationComponent.Companion(
            componentContext = componentContext,
            onBack = onBack,
        )

    fun createMandatoryLogin(componentContext: ComponentContext): MandatoryLoginComponent =
        MandatoryLoginComponent.Companion(
            componentContext = componentContext,
            onNavigateToCountrySelector = {
                loginCoordinator.navigateToCountrySelector { country ->
                    loginCoordinator.loginViewModel.onCountrySelected(country)
                }
            },
            onNavigateToOtpVerification = { loginCoordinator.navigateToOtpVerification() },
        )

    fun createTournamentGame(
        componentContext: ComponentContext,
        tournamentId: String,
        tournamentTitle: String,
        initialDiamonds: Int,
        startEpochMs: Long,
        endEpochMs: Long,
        totalPrizePool: Int,
        isHotOrNot: Boolean,
    ): TournamentGameComponent =
        TournamentGameComponent(
            componentContext = componentContext,
            requestLoginFactory = rootComponent.createLoginRequestFactory(),
            tournamentId = tournamentId,
            tournamentTitle = tournamentTitle,
            initialDiamonds = initialDiamonds,
            totalPrizePool = totalPrizePool,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            isHotOrNot = isHotOrNot,
            onLeaderboardClick = { clickedTournamentId, showResult ->
                rootComponent.openTournamentLeaderboard(clickedTournamentId, showResult)
            },
            onTimeUp = {
                rootComponent.openTournamentResults(tournamentId, true)
            },
            onBack = rootComponent::onBackClicked,
        )
}
