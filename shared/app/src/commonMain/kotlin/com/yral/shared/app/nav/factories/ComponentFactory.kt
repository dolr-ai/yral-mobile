package com.yral.shared.app.nav.factories

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.app.nav.Config
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.nav.SplashComponent
import com.yral.shared.app.ui.screens.home.nav.HomeComponent
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.aiinfluencer.nav.CreateInfluencerComponent
import com.yral.shared.features.auth.nav.countryselector.CountrySelectorComponent
import com.yral.shared.features.auth.nav.mandatorylogin.MandatoryLoginComponent
import com.yral.shared.features.auth.nav.otpverification.OtpVerificationComponent
import com.yral.shared.features.auth.ui.LoginCoordinator
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.features.subscriptions.nav.SubscriptionsComponent
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.phonevalidation.countries.Country
import com.yral.shared.libs.routing.routes.api.Profile
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
                subscriptionCoordinator = rootComponent.getSubscriptionCoordinator(),
                openEditProfile = rootComponent::openEditProfile,
                openProfile = rootComponent::openProfile,
                openCreateInfluencer = rootComponent::openCreateInfluencer,
                openConversation = rootComponent::openConversation,
                openWallet = rootComponent::openWallet,
                openAccountSheet = { rootComponent.rootViewModel.showAccountSwitcher() },
                switchToMainProfile = { onComplete ->
                    rootComponent.rootViewModel.switchToMainAccount(onComplete)
                },
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

    fun createCreateInfluencer(
        componentContext: ComponentContext,
        config: Config.CreateInfluencer,
    ): CreateInfluencerComponent =
        CreateInfluencerComponent(
            componentContext = componentContext,
            source = config.source,
            requestLoginFactory = rootComponent.createLoginRequestFactory(),
            onBack = rootComponent::onBackClicked,
            onProfileCreated = { successMessage ->
                rootComponent.onBackClicked()
                rootComponent.onNavigationRequest(Profile)
                ToastManager.showSuccess(type = ToastType.Small(successMessage))
            },
        )

    fun createProfile(
        componentContext: ComponentContext,
        config: Config.UserProfile,
    ): ProfileMainComponent =
        ProfileMainComponent.invoke(
            componentContext = componentContext,
            requestLoginFactory = rootComponent.createLoginRequestFactory(),
            subscriptionCoordinator = rootComponent.getSubscriptionCoordinator(),
            userCanisterData = config.userCanisterData,
            pendingVideoNavigation = flowOf(null),
            onUploadVideoClicked = {},
            openAccountSheet = { rootComponent.rootViewModel.showAccountSwitcher() },
            openAccount = {},
            openEditProfile = {},
            openProfile = rootComponent::openProfile,
            openCreateInfluencer = rootComponent::openCreateInfluencer,
            openConversation = rootComponent::openConversation,
            onBackClicked = rootComponent::onBackClicked,
            showAlertsOnDialog = showAlertsOnDialog,
            showBackButton = true,
        )

    fun createConversation(
        componentContext: ComponentContext,
        config: Config.Conversation,
    ): ConversationComponent =
        ConversationComponent.Companion(
            componentContext = componentContext,
            requestLoginFactory = rootComponent.createLoginRequestFactory(),
            subscriptionCoordinator = rootComponent.getSubscriptionCoordinator(),
            openConversationParams = config.params,
            onBack = rootComponent::onBackClicked,
            openProfile = rootComponent::openProfile,
        )

    fun createWallet(componentContext: ComponentContext): WalletComponent =
        WalletComponent(
            componentContext = componentContext,
            showAlertsOnDialog = showAlertsOnDialog,
            showBackIcon = true,
            onBack = rootComponent::onBackClicked,
            onCreateInfluencer = { rootComponent.openCreateInfluencer(BotCreationSource.WALLET) },
            onSwitchProfile = { rootComponent.rootViewModel.showAccountSwitcher() },
            onOpenProfile = rootComponent::openProfile,
        )

    fun createSubscription(
        componentContext: ComponentContext,
        config: Config.Subscription,
    ): SubscriptionsComponent =
        SubscriptionsComponent.Companion(
            componentContext = componentContext,
            purchaseTimeMs = config.purchaseTimeMs,
            entryPoint = config.entryPoint,
            onBack = rootComponent::onBackClicked,
            onCreateVideo = rootComponent::onCreateVideo,
            onExploreFeed = rootComponent::onExploreFeed,
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
}
