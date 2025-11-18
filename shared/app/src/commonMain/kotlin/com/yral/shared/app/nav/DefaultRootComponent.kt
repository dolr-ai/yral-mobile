package com.yral.shared.app.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.app.UpdateState
import com.yral.shared.app.ui.screens.alertsrequest.nav.AlertsRequestComponent
import com.yral.shared.app.ui.screens.home.nav.HomeComponent
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.features.root.viewmodels.RootViewModel
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent,
    ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()
    private var homeComponent: HomeComponent? = null

    private var pendingNavRoute: AppRoute? = null

    private val _updateState = MutableValue<UpdateState>(UpdateState.Idle)
    override val updateState: Value<UpdateState> = _updateState

    private var onCompleteUpdateCallback: (() -> Unit)? = null

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::child,
        ).also { stackValue ->
            // Observe stack changes to handle pending navigation
            stackValue.subscribe { stack ->
                val navRoute = pendingNavRoute
                if (navRoute != null && stack.active.instance is RootComponent.Child.Home) {
                    pendingNavRoute = null
                    homeComponent?.onNavigationRequest(navRoute)
                }
            }
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): RootComponent.Child =
        when (config) {
            is Config.Splash -> RootComponent.Child.Splash(splashComponent(componentContext))
            is Config.Home -> RootComponent.Child.Home(homeComponent(componentContext))
            is Config.EditProfile -> RootComponent.Child.EditProfile(editProfileComponent(componentContext))
            is Config.UserProfile -> RootComponent.Child.UserProfile(profileComponent(componentContext, config))
        }

    private val slotNavigation = SlotNavigation<SlotConfig>()
    private var loginSlotCallbacks: LoginSlotCallbacks? = null
    private val rootViewModel: RootViewModel = koinInstance.get<RootViewModel>()

    override val slot: Value<ChildSlot<*, RootComponent.SlotChild>> =
        childSlot(
            source = slotNavigation,
            serializer = SlotConfig.serializer(),
            handleBackButton = true,
            childFactory = ::slotChild,
        )

    private fun splashComponent(componentContext: ComponentContext): SplashComponent =
        SplashComponent(
            componentContext = componentContext,
        )

    private fun homeComponent(componentContext: ComponentContext): HomeComponent {
        val component =
            HomeComponent.Companion(
                componentContext = componentContext,
                openEditProfile = this::openEditProfile,
                openProfile = this::openProfile,
                showAlertsOnDialog = { this.showSlot(SlotConfig.AlertsRequestBottomSheet(it)) },
                showLoginBottomSheet = this::showLoginBottomSheet,
                hideLoginBottomSheetIfVisible = this::hideLoginBottomSheetIfVisible,
            )
        homeComponent = component
        return component
    }

    private fun editProfileComponent(componentContext: ComponentContext): EditProfileComponent =
        EditProfileComponent.Companion(
            componentContext = componentContext,
            onBack = this::onBackClicked,
        )

    private fun profileComponent(
        componentContext: ComponentContext,
        config: Config.UserProfile,
    ): ProfileMainComponent =
        ProfileMainComponent.invoke(
            componentContext = componentContext,
            userCanisterData = config.userCanisterData,
            pendingVideoNavigation = flowOf(null),
            onUploadVideoClicked = {},
            openAccount = {},
            openEditProfile = {},
            openProfile = this::openProfile,
            onBackClicked = this::onBackClicked,
            showAlertsOnDialog = { this.showSlot(SlotConfig.AlertsRequestBottomSheet(it)) },
            promptLogin = {
                val termsLink = rootViewModel.getTncLink()
                showLoginBottomSheet(
                    pageName = SignupPageName.MENU,
                    headlineText = null,
                    termsLink = termsLink,
                    onDismissRequest = { hideLoginBottomSheetIfVisible() },
                    onLoginSuccess = { hideLoginBottomSheetIfVisible() },
                )
            },
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    override fun setSplashActive(active: Boolean) {
        if (active == isSplashActive()) return
        val config = if (active) Config.Splash else Config.Home
        navigation.replaceAll(config)
    }

    override fun isSplashActive(): Boolean = stack.active.instance is RootComponent.Child.Splash

    override fun onNavigationRequest(appRoute: AppRoute) {
        if (isSplashActive()) {
            pendingNavRoute = appRoute
        } else {
            homeComponent?.onNavigationRequest(appRoute)
        }
    }

    override fun onUpdateStateChanged(state: UpdateState) {
        _updateState.value = state
    }

    override fun onCompleteUpdateClicked() {
        onCompleteUpdateCallback?.invoke()
    }

    fun setOnCompleteUpdateCallback(callback: () -> Unit) {
        onCompleteUpdateCallback = callback
    }

    override fun openEditProfile() {
        navigation.pushToFront(Config.EditProfile)
    }

    override fun openProfile(userCanisterData: CanisterData) {
        navigation.pushToFront(Config.UserProfile(userCanisterData))
    }

    override fun showLoginBottomSheet(
        pageName: SignupPageName,
        headlineText: String?,
        termsLink: String,
        onDismissRequest: () -> Unit,
        onLoginSuccess: () -> Unit,
    ) {
        loginSlotCallbacks =
            LoginSlotCallbacks(
                onDismissRequest = onDismissRequest,
                onLoginSuccess = onLoginSuccess,
            )
        showSlot(
            SlotConfig.LoginBottomSheet(
                pageName = pageName,
                headlineText = headlineText,
                termsLink = termsLink,
            ),
        )
    }

    override fun hideLoginBottomSheetIfVisible() {
        val currentConfig = slot.value.child?.configuration
        if (currentConfig is SlotConfig.LoginBottomSheet) {
            loginSlotCallbacks = null
            slotNavigation.dismiss()
        }
    }

    private fun slotChild(
        config: SlotConfig,
        componentContext: ComponentContext,
    ): RootComponent.SlotChild =
        when (config) {
            is SlotConfig.AlertsRequestBottomSheet ->
                RootComponent.SlotChild.AlertsRequestBottomSheet(
                    component = alertsRequestComponent(componentContext, config.requestType),
                )
            is SlotConfig.LoginBottomSheet ->
                RootComponent.SlotChild.LoginBottomSheet(
                    pageName = config.pageName,
                    headlineText = config.headlineText,
                    termsLink = config.termsLink,
                    onDismissRequest = {
                        loginSlotCallbacks?.onDismissRequest?.invoke()
                        loginSlotCallbacks = null
                        slotNavigation.dismiss()
                    },
                    onLoginSuccess = {
                        loginSlotCallbacks?.onLoginSuccess?.invoke()
                        loginSlotCallbacks = null
                        slotNavigation.dismiss()
                    },
                )
        }

    private fun alertsRequestComponent(
        componentContext: ComponentContext,
        type: AlertsRequestType,
    ): AlertsRequestComponent =
        AlertsRequestComponent(
            componentContext = componentContext,
            type = type,
            onDismissed = slotNavigation::dismiss,
        )

    private fun showSlot(slotConfig: SlotConfig) {
        slotNavigation.activate(slotConfig)
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Splash : Config

        @Serializable
        data object Home : Config

        @Serializable
        data object EditProfile : Config

        @Serializable
        data class UserProfile(
            val userCanisterData: CanisterData,
        ) : Config
    }

    @Serializable
    private sealed interface SlotConfig {
        @Serializable
        data class AlertsRequestBottomSheet(
            val requestType: AlertsRequestType,
        ) : SlotConfig

        @Serializable
        data class LoginBottomSheet(
            val pageName: SignupPageName,
            val headlineText: String?,
            val termsLink: String,
        ) : SlotConfig
    }

    private data class LoginSlotCallbacks(
        val onDismissRequest: () -> Unit,
        val onLoginSuccess: () -> Unit,
    )
}
