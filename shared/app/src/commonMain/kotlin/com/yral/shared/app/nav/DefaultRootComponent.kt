package com.yral.shared.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.yral.shared.app.UpdateState
import com.yral.shared.app.nav.factories.ComponentFactory
import com.yral.shared.app.ui.screens.alertsrequest.nav.AlertsRequestComponent
import com.yral.shared.app.ui.screens.home.nav.HomeComponent
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.auth.ui.LoginCoordinator
import com.yral.shared.features.auth.ui.LoginInfo
import com.yral.shared.features.auth.ui.LoginOverlay
import com.yral.shared.features.auth.ui.LoginScreenType
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.auth.ui.toRequestFactory
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.phonevalidation.countries.Country
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.Profile
import com.yral.shared.libs.routing.routes.api.UserProfileRoute
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister

@Suppress("TooManyFunctions")
class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent,
    LoginCoordinator,
    ComponentContext by componentContext {
    // ==================== Navigation ====================
    private val navigation = StackNavigation<Config>()
    private val slotNavigation = SlotNavigation<SlotConfig>()
    private var homeComponent: HomeComponent? = null

    // ==================== Dependencies ====================
    private val sessionManager: SessionManager = koinInstance.get()
    override val loginViewModel: LoginViewModel = koinInstance.get()

    // ==================== State ====================
    private var pendingNavRoute: AppRoute? = null
    private val _updateState = MutableValue<UpdateState>(UpdateState.Idle)
    override val updateState: Value<UpdateState> = _updateState
    private var onCompleteUpdateCallback: (() -> Unit)? = null

    // ==================== Login State (preserved across configuration changes) ====================
    // Holder class to preserve login state across configuration changes
    private class LoginStateHolder : InstanceKeeper.Instance {
        var currentLoginInfo: LoginInfo? = null
        var countrySelectionCallback: ((Country) -> Unit)? = null
    }

    private val loginStateHolder: LoginStateHolder =
        instanceKeeper.getOrCreate("loginState") { LoginStateHolder() }

    // Current active login request. Null when no login is in progress.
    override var currentLoginInfo: LoginInfo?
        get() = loginStateHolder.currentLoginInfo
        set(value) {
            loginStateHolder.currentLoginInfo = value
        }

    // Callback for country selection during phone auth flow.
    private var countrySelectionCallback: ((Country) -> Unit)?
        get() = loginStateHolder.countrySelectionCallback
        set(value) {
            loginStateHolder.countrySelectionCallback = value
        }

    // ==================== Component Factory ====================
    private val componentFactory =
        ComponentFactory(
            rootComponent = this,
            loginCoordinator = this,
            setHomeComponent = { homeComponent = it },
            showAlertsOnDialog = ::showAlertsSlot,
            onFeedTabClick = { homeComponent?.onFeedTabClick() },
        )

    // ==================== Navigation Stacks ====================
    // need to declare slot before stack since onStackChange needs slot
    override val slot: Value<ChildSlot<*, RootComponent.SlotChild>> =
        childSlot(
            source = slotNavigation,
            serializer = SlotConfig.serializer(),
            handleBackButton = true,
            childFactory = ::createSlotChild,
        )

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::createChild,
        ).also { stackValue ->
            stackValue.subscribe(::onStackChanged)
        }

    // ==================== Stack Change Handler ====================
    private fun onStackChanged(childStack: ChildStack<*, RootComponent.Child>) {
        val currentChild = childStack.active.instance
        // Handle pending navigation when Home becomes active
        pendingNavRoute?.let { route ->
            if (currentChild is RootComponent.Child.Home) {
                pendingNavRoute = null
                handleAppRoute(route)
            }
        }
    }

    // ==================== Child Factories ====================
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun createChild(
        config: Config,
        context: ComponentContext,
    ): RootComponent.Child =
        when (config) {
            is Config.Splash -> RootComponent.Child.Splash(componentFactory.createSplash(context))
            is Config.Home -> RootComponent.Child.Home(componentFactory.createHome(context))
            is Config.EditProfile -> RootComponent.Child.EditProfile(componentFactory.createEditProfile(context))
            is Config.UserProfile -> RootComponent.Child.UserProfile(componentFactory.createProfile(context, config))
            is Config.TournamentLeaderboard ->
                RootComponent.Child.TournamentLeaderboard(
                    tournamentId = config.tournamentId,
                    showResult = config.showResult,
                )
            is Config.TournamentGame ->
                RootComponent.Child.TournamentGame(
                    componentFactory.createTournamentGame(
                        context,
                        config.tournamentId,
                        config.tournamentTitle,
                        config.initialDiamonds,
                        config.startEpochMs,
                        config.endEpochMs,
                        config.totalPrizePool,
                    ),
                )
            is Config.Conversation ->
                RootComponent.Child.Conversation(
                    componentFactory.createConversation(context, config),
                )
            is Config.Wallet -> RootComponent.Child.Wallet(componentFactory.createWallet(context))
            is Config.Leaderboard -> RootComponent.Child.Leaderboard(componentFactory.createLeaderboard(context))
            is Config.CountrySelector ->
                RootComponent.Child.CountrySelector(
                    componentFactory.createCountrySelector(
                        componentContext = context,
                        onCountrySelected = { country ->
                            countrySelectionCallback?.invoke(country)
                            navigation.pop()
                            if (currentLoginInfo?.screenType is LoginScreenType.BottomSheet) {
                                showLoginSlot(currentLoginInfo!!)
                            }
                        },
                        onBack = {
                            navigation.pop()
                            if (currentLoginInfo?.screenType is LoginScreenType.BottomSheet) {
                                showLoginSlot(currentLoginInfo!!)
                            }
                        },
                    ),
                )
            is Config.OtpVerification ->
                RootComponent.Child.OtpVerification(
                    componentFactory.createOtpVerification(
                        componentContext = context,
                        onBack = {
                            loginViewModel.resetState()
                            navigation.pop()
                            if (currentLoginInfo?.screenType is LoginScreenType.BottomSheet) {
                                showLoginSlot(currentLoginInfo!!)
                            }
                        },
                    ),
                )
            is Config.MandatoryLogin ->
                RootComponent.Child.MandatoryLogin(
                    componentFactory.createMandatoryLogin(context),
                )
        }

    private fun createSlotChild(
        config: SlotConfig,
        context: ComponentContext,
    ): RootComponent.SlotChild =
        when (config) {
            is SlotConfig.AlertsRequestBottomSheet ->
                RootComponent.SlotChild.AlertsRequestBottomSheet(
                    component =
                        AlertsRequestComponent(
                            componentContext = context,
                            type = config.requestType,
                            onDismissed = slotNavigation::dismiss,
                        ),
                )
            is SlotConfig.LoginBottomSheet -> {
                currentLoginInfo
                    ?.let { RootComponent.SlotChild.LoginBottomSheet() }
                    ?: error("LoginInfo not available for slot")
            }
        }

    // ==================== RootComponent Navigation ====================
    override fun onBackClicked() {
        navigation.pop()
    }

    override fun isSplashActive(): Boolean = stack.active.instance is RootComponent.Child.Splash

    override fun isMandatoryLoginActive(): Boolean = stack.active.instance is RootComponent.Child.MandatoryLogin

    override fun navigateToSplash() {
        if (!isSplashActive()) navigation.replaceAll(Config.Splash)
    }

    override fun navigateToMandatoryLogin() {
        if (!isMandatoryLoginActive()) navigation.replaceAll(Config.MandatoryLogin)
    }

    override fun navigateToHome() {
        if (stack.active.instance !is RootComponent.Child.Home) {
            navigation.replaceAll(Config.Home)
        }
    }

    override fun onNavigationRequest(appRoute: AppRoute) {
        if (isSplashActive() || isMandatoryLoginActive()) {
            pendingNavRoute = appRoute
            return
        }
        handleAppRoute(appRoute)
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

    // ==================== Route Handling (inline) ====================
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

    private fun handleUserProfileRoute(route: UserProfileRoute) {
        val currentUser = sessionManager.userPrincipal
        if (!currentUser.isNullOrBlank() && currentUser == route.userPrincipalId) {
            homeComponent?.onNavigationRequest(Profile) ?: run {
                pendingNavRoute = Profile
                navigation.replaceAll(Config.Home)
            }
            return
        }
        openProfile(
            CanisterData(
                canisterId = route.canisterId,
                userPrincipalId = route.userPrincipalId,
                profilePic = route.profilePic ?: "",
                username = route.username,
                isCreatedFromServiceCanister =
                    route.isFromServiceCanister ||
                        route.canisterId == getUserInfoServiceCanister(),
                isFollowing = false,
            ),
        )
    }

    // ==================== Screen Navigation ====================
    override fun openEditProfile() {
        navigation.pushToFront(Config.EditProfile)
    }

    override fun openProfile(userCanisterData: CanisterData) {
        navigation.pushToFront(Config.UserProfile(userCanisterData))
    }

    override fun openTournamentLeaderboard(
        tournamentId: String,
        showResult: Boolean,
    ) {
        navigation.pushToFront(Config.TournamentLeaderboard(tournamentId, showResult))
    }

    override fun openTournamentGame(
        tournamentId: String,
        tournamentTitle: String,
        initialDiamonds: Int,
        startEpochMs: Long,
        endEpochMs: Long,
        totalPrizePool: Int,
    ) {
        navigation.pushToFront(
            Config.TournamentGame(
                tournamentId,
                tournamentTitle,
                initialDiamonds,
                startEpochMs,
                endEpochMs,
                totalPrizePool,
            ),
        )
    }

    override fun openConversation(
        influencerId: String,
        influencerCategory: String,
    ) {
        navigation.pushToFront(Config.Conversation(influencerId, influencerCategory))
    }

    override fun openWallet() {
        navigation.pushToFront(Config.Wallet)
    }

    override fun openLeaderboard() {
        navigation.pushToFront(Config.Leaderboard)
    }

    // ==================== LoginCoordinator Implementation ====================
    override fun requestLogin(loginInfo: LoginInfo): @Composable () -> Unit {
        // Store the login info
        currentLoginInfo = loginInfo

        // Activate bottom sheet slot if needed
        if (loginInfo.screenType is LoginScreenType.BottomSheet) {
            showLoginSlot(loginInfo)
        }

        // Return composable based on screen type
        return when (loginInfo.screenType) {
            is LoginScreenType.Overlay -> {
                {
                    // Watch for auth success
                    val state by loginViewModel.state.collectAsState()
                    LaunchedEffect(state) {
                        if (state.isLoginComplete()) {
                            loginInfo.onSuccess?.invoke()
                            clearLoginState()
                            loginViewModel.resetState()
                        }
                    }
                    LoginOverlay(
                        pageName = loginInfo.pageName,
                        tncLink = loginViewModel.getTncLink(),
                        mode = loginInfo.mode,
                        onNavigateToCountrySelector = {
                            navigateToCountrySelector { loginViewModel.onCountrySelected(it) }
                        },
                        onNavigateToOtpVerification = { navigateToOtpVerification() },
                        bottomContent = loginInfo.bottomContent,
                    )
                }
            }
            is LoginScreenType.BottomSheet -> {
                { /* Rendered via slot */ }
            }
        }
    }

    override fun navigateToCountrySelector(onCountrySelected: (Country) -> Unit) {
        countrySelectionCallback = onCountrySelected
        dismissLoginSlotIfActive()
        navigation.pushToFront(Config.CountrySelector)
    }

    override fun navigateToOtpVerification() {
        dismissLoginSlotIfActive()
        navigation.pushToFront(Config.OtpVerification)
    }

    // ==================== Login Slot Helpers ====================
    private fun showLoginSlot(loginInfo: LoginInfo) {
        if (loginInfo.screenType !is LoginScreenType.BottomSheet) return
        slotNavigation.activate(SlotConfig.LoginBottomSheet)
    }

    private fun dismissLoginSlotIfActive() {
        if (slot.value.child?.instance is RootComponent.SlotChild.LoginBottomSheet) {
            slotNavigation.dismiss()
        }
    }

    private fun showAlertsSlot(type: AlertsRequestType) {
        slotNavigation.activate(SlotConfig.AlertsRequestBottomSheet(type))
    }

    override fun clearLoginState() {
        currentLoginInfo = null
        countrySelectionCallback = null
    }

    // Called by UI when bottom sheet is dismissed by user
    override fun dismissLoginBottomSheet() {
        slotNavigation.dismiss()
        currentLoginInfo?.onDismiss?.invoke()
        clearLoginState()
        loginViewModel.resetState()
    }

    // ==================== LoginCoordinator Access ====================
    override fun getLoginCoordinator(): LoginCoordinator = this

    override fun createLoginRequestFactory(): RequestLoginFactory = this.toRequestFactory()
}
