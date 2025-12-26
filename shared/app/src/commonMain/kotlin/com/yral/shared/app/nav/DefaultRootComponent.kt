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
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.features.tournament.nav.TournamentGameComponent
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.UserProfileRoute
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable

@Suppress("TooManyFunctions")
class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent,
    ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()
    private var homeComponent: HomeComponent? = null
    private val sessionManager: SessionManager = koinInstance.get()

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
                    handleAppRoute(navRoute)
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
            is Config.TournamentLeaderboard ->
                RootComponent.Child.TournamentLeaderboard(
                    tournamentId = config.tournamentId,
                    participantsLabel = config.participantsLabel,
                    scheduleLabel = config.scheduleLabel,
                )
            is Config.TournamentGame ->
                RootComponent.Child.TournamentGame(
                    tournamentGameComponent(
                        componentContext,
                        config.tournamentId,
                        config.tournamentTitle,
                        config.initialDiamonds,
                        config.endEpochMs,
                        config.totalPrizePool,
                    ),
                )
        }

    private val slotNavigation = SlotNavigation<SlotConfig>()
    private var loginSlotCallbacks: LoginSlotCallbacks? = null

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
                openTournamentLeaderboard = this::openTournamentLeaderboard,
                openTournamentGame = this::openTournamentGame,
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
                showLoginBottomSheet(
                    pageName = SignupPageName.PROFILE,
                    loginBottomSheetType = LoginBottomSheetType.DEFAULT,
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
            return
        }
        handleAppRoute(appRoute)
    }

    override fun onUpdateStateChanged(state: UpdateState) {
        _updateState.value = state
    }

    private fun handleAppRoute(appRoute: AppRoute) {
        when (appRoute) {
            is UserProfileRoute -> handleUserProfileRoute(appRoute)
            else ->
                homeComponent?.onNavigationRequest(appRoute) ?: run {
                    pendingNavRoute = appRoute
                    navigation.replaceAll(Config.Home)
                }
        }
    }

    private fun handleUserProfileRoute(appRoute: UserProfileRoute) {
        val currentUser = sessionManager.userPrincipal
        if (!currentUser.isNullOrBlank() && currentUser == appRoute.userPrincipalId) {
            // Navigate to Profile tab inside Home to keep bottom nav visible
            homeComponent?.onNavigationRequest(com.yral.shared.libs.routing.routes.api.Profile)
                ?: run {
                    pendingNavRoute = com.yral.shared.libs.routing.routes.api.Profile
                    navigation.replaceAll(Config.Home)
                }
            return
        }
        val canisterData =
            CanisterData(
                canisterId = appRoute.canisterId,
                userPrincipalId = appRoute.userPrincipalId,
                profilePic = appRoute.profilePic ?: "",
                username = appRoute.username,
                isCreatedFromServiceCanister =
                    appRoute.isFromServiceCanister ||
                        appRoute.canisterId == getUserInfoServiceCanister(),
                isFollowing = false,
            )
        openProfile(canisterData)
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

    override fun openTournamentLeaderboard(
        tournamentId: String,
        participantsLabel: String,
        scheduleLabel: String,
    ) {
        navigation.pushToFront(
            Config.TournamentLeaderboard(
                tournamentId = tournamentId,
                participantsLabel = participantsLabel,
                scheduleLabel = scheduleLabel,
            ),
        )
    }

    override fun openTournamentGame(
        tournamentId: String,
        tournamentTitle: String,
        initialDiamonds: Int,
        endEpochMs: Long,
        totalPrizePool: Int,
    ) {
        navigation.pushToFront(
            Config.TournamentGame(
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle,
                initialDiamonds = initialDiamonds,
                endEpochMs = endEpochMs,
                totalPrizePool = totalPrizePool,
            ),
        )
    }

    override fun showLoginBottomSheet(
        pageName: SignupPageName,
        loginBottomSheetType: LoginBottomSheetType,
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
                loginBottomSheetType = loginBottomSheetType,
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
                    loginBottomSheetType = config.loginBottomSheetType,
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

    private fun tournamentGameComponent(
        componentContext: ComponentContext,
        tournamentId: String,
        tournamentTitle: String,
        initialDiamonds: Int,
        endEpochMs: Long,
        totalPrizePool: Int,
    ): TournamentGameComponent =
        TournamentGameComponent(
            componentContext = componentContext,
            tournamentId = tournamentId,
            tournamentTitle = tournamentTitle,
            initialDiamonds = initialDiamonds,
            totalPrizePool = totalPrizePool,
            endEpochMs = endEpochMs,
            onLeaderboardClick = { clickedTournamentId ->
                openTournamentLeaderboard(
                    tournamentId = clickedTournamentId,
                    participantsLabel = "",
                    scheduleLabel = "",
                )
            },
            onTimeUp = { navigation.pop() },
            onBack = { navigation.pop() },
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

        @Serializable
        data class TournamentLeaderboard(
            val tournamentId: String,
            val participantsLabel: String,
            val scheduleLabel: String,
        ) : Config

        @Serializable
        data class TournamentGame(
            val tournamentId: String,
            val tournamentTitle: String = "",
            val initialDiamonds: Int,
            val endEpochMs: Long,
            val totalPrizePool: Int,
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
            val loginBottomSheetType: LoginBottomSheetType,
        ) : SlotConfig
    }

    private data class LoginSlotCallbacks(
        val onDismissRequest: () -> Unit,
        val onLoginSuccess: () -> Unit,
    )
}
