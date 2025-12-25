package com.yral.shared.app.ui.screens.home.nav

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.app.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.account.nav.AccountComponent
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.chat.nav.ChatComponent
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.root.viewmodels.HomeViewModel
import com.yral.shared.features.tournament.nav.TournamentComponent
import com.yral.shared.features.tournament.nav.TournamentGameComponent
import com.yral.shared.features.uploadvideo.nav.UploadVideoRootComponent
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.features.wallet.ui.btcRewards.nav.DefaultVideoViewRewardsComponent
import com.yral.shared.features.wallet.ui.btcRewards.nav.VideoViewRewardsComponent
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.arch.nav.HomeChildSnapshotProvider
import com.yral.shared.libs.routing.routes.api.AddVideo
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.GenerateAIVideo
import com.yral.shared.libs.routing.routes.api.Leaderboard
import com.yral.shared.libs.routing.routes.api.PendingAppRouteStore
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import com.yral.shared.libs.routing.routes.api.Profile
import com.yral.shared.libs.routing.routes.api.RewardOn
import com.yral.shared.libs.routing.routes.api.RewardsReceived
import com.yral.shared.libs.routing.routes.api.Tournaments
import com.yral.shared.libs.routing.routes.api.VideoUploadSuccessful
import com.yral.shared.libs.routing.routes.api.Wallet
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.serialization.Serializable

@Suppress("TooManyFunctions")
internal class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val openEditProfile: () -> Unit,
    private val openProfile: (userCanisterData: CanisterData) -> Unit,
    private val openTournamentLeaderboard: (
        tournamentId: String,
        participantsLabel: String,
        scheduleLabel: String,
    ) -> Unit,
    override val showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
    private val showLoginBottomSheet: (
        pageName: SignupPageName,
        loginBottomSheetType: LoginBottomSheetType,
        onDismissRequest: () -> Unit,
        onLoginSuccess: () -> Unit,
    ) -> Unit,
    private val hideLoginBottomSheetIfVisible: () -> Unit,
) : HomeComponent(),
    ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()
    private val childSnapshots: MutableMap<Config, Any> = LinkedHashMap()
    private var lastActiveConfig: Config? = null
    private var lastActiveProvider: HomeChildSnapshotProvider? = null

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Feed,
            handleBackButton = true,
            childFactory = ::child,
        ).also { stackValue ->
            stackValue.subscribe { current ->
                val activeChild = current.active.instance
                val (newConfig, newProvider) = mapActiveChild(activeChild)
                if (lastActiveConfig != newConfig) {
                    lastActiveProvider?.let { provider ->
                        lastActiveConfig?.let { prevConfig ->
                            childSnapshots[prevConfig] = provider.createHomeSnapshot()
                        }
                    }
                    lastActiveConfig = newConfig
                    lastActiveProvider = newProvider
                }
            }
        }

    override val homeViewModel: HomeViewModel = koinInstance.get<HomeViewModel>()
    override val sessionManager: SessionManager = koinInstance.get<SessionManager>()

    private val slotNavigation = SlotNavigation<SlotConfig>()

    override val slot: Value<ChildSlot<*, SlotChild>> =
        childSlot(
            source = slotNavigation,
            serializer = SlotConfig.serializer(),
            handleBackButton = true,
            childFactory = ::slotChild,
        )

    override fun onFeedTabClick() {
        navigation.replaceAll(Config.Feed)
    }

    override fun onLeaderboardTabClick() {
        navigation.replaceKeepingFeed(Config.Leaderboard)
    }

    override fun onTournamentTabClick() {
        navigation.replaceKeepingFeed(Config.Tournament)
    }

    override fun onUploadVideoTabClick() {
        navigation.replaceKeepingFeed(Config.UploadVideo)
    }

    override fun onProfileTabClick() {
        navigation.replaceKeepingFeed(Config.Profile)
    }

    override fun onNavigationRequest(appRoute: AppRoute) {
        when (appRoute) {
            is PostDetailsRoute ->
                navigation
                    .replaceAll(Config.Feed) {
                        (stack.value.active.instance as? Child.Feed)?.component?.openPostDetails(
                            appRoute,
                        )
                    }.also { Logger.d("LinkSharing") { "Link details received $appRoute" } }

            is Wallet -> onWalletTabClick()
            is Leaderboard -> onLeaderboardTabClick()
            is Tournaments -> onTournamentTabClick()
            is Profile -> onProfileTabClick()
            is AddVideo -> onUploadVideoTabClick()
            is GenerateAIVideo ->
                navigation.replaceKeepingFeed(Config.UploadVideo) {
                    (stack.value.active.instance as? Child.UploadVideo)?.component?.handleNavigation(
                        appRoute,
                    )
                }

            is RewardsReceived -> {
                when (appRoute.rewardOn) {
                    RewardOn.VIDEO_VIEWS ->
                        showSlot(
                            SlotConfig.VideoViewsRewardsBottomSheet(
                                appRoute,
                            ),
                        )
                }
            }

            is VideoUploadSuccessful ->
                navigation.replaceKeepingFeed(Config.Profile) {
                    (stack.value.active.instance as? Child.Profile)?.component?.onNavigationRequest(
                        appRoute,
                    )
                }

            else -> Unit
        }
    }

    override fun onAccountTabClick() {
        navigation.replaceKeepingFeed(Config.Account)
    }

    override fun onWalletTabClick() {
        navigation.replaceKeepingFeed(Config.Wallet)
    }

    override fun onChatTabClick() {
        navigation.replaceKeepingFeed(Config.Chat)
    }

    override fun showLoginBottomSheet(
        pageName: SignupPageName,
        loginBottomSheetType: LoginBottomSheetType,
        onDismissRequest: () -> Unit,
        onLoginSuccess: () -> Unit,
    ) {
        showLoginBottomSheet.invoke(
            pageName,
            loginBottomSheetType,
            onDismissRequest,
            onLoginSuccess,
        )
    }

    override fun hideLoginBottomSheetIfVisible() {
        hideLoginBottomSheetIfVisible.invoke()
    }

    private inline fun StackNavigator<Config>.replaceKeepingFeed(
        configuration: Config,
        crossinline onComplete: () -> Unit = { },
    ) {
        replaceAll(Config.Feed, configuration, onComplete = onComplete)
    }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            is Config.Feed -> Child.Feed(feedComponent(componentContext))
            is Config.Leaderboard -> Child.Leaderboard(leaderboardComponent(componentContext))
            is Config.Tournament -> Child.Tournament(tournamentComponent(componentContext))
            is Config.TournamentGame ->
                Child.TournamentGame(
                    tournamentGameComponent(
                        componentContext,
                        config.tournamentId,
                        config.tournamentTitle,
                        config.initialDiamonds,
                        config.endEpochMs,
                        config.totalPrizePool,
                    ),
                )
            is Config.UploadVideo -> Child.UploadVideo(uploadVideoComponent(componentContext))
            is Config.Profile -> Child.Profile(profileComponent(componentContext))
            is Config.Account -> Child.Account(accountComponent(componentContext))
            is Config.Wallet -> Child.Wallet(walletComponent(componentContext))
            is Config.Chat -> Child.Chat(chatComponent(componentContext))
        }

    private fun mapActiveChild(child: Child): Pair<Config, HomeChildSnapshotProvider?> =
        when (child) {
            is Child.Feed -> Config.Feed to (child.component as? HomeChildSnapshotProvider)
            is Child.Leaderboard -> Config.Leaderboard to (child.component as? HomeChildSnapshotProvider)
            is Child.Tournament -> Config.Tournament to (child.component as? HomeChildSnapshotProvider)
            is Child.TournamentGame ->
                Config.TournamentGame(
                    child.component.gameConfig.tournamentId,
                    child.component.gameConfig.tournamentTitle,
                    child.component.gameConfig.initialDiamonds,
                    child.component.gameConfig.endEpochMs,
                    child.component.gameConfig.totalPrizePool,
                ) to null
            is Child.UploadVideo -> Config.UploadVideo to child.component
            is Child.Profile -> Config.Profile to (child.component as? HomeChildSnapshotProvider)
            is Child.Account -> Config.Account to (child.component as? HomeChildSnapshotProvider)
            is Child.Wallet -> Config.Wallet to (child.component as? HomeChildSnapshotProvider)
            is Child.Chat -> Config.Chat to (child.component as? HomeChildSnapshotProvider)
        }

    private fun feedComponent(componentContext: ComponentContext): FeedComponent =
        FeedComponent.Companion(
            componentContext = componentContext,
            openProfile = openProfile,
            showAlertsOnDialog = showAlertsOnDialog,
            promptLogin = {
                PendingAppRouteStore.store(it)
                homeViewModel.showSignupPrompt(true, SignupPageName.HOME)
            },
            openLeaderboard = { onLeaderboardTabClick() },
            openWallet = { onWalletTabClick() },
        )

    private fun leaderboardComponent(componentContext: ComponentContext): LeaderboardComponent =
        LeaderboardComponent.Companion(
            componentContext = componentContext,
            snapshot = childSnapshots[Config.Leaderboard] as? LeaderboardComponent.Snapshot,
            navigateToHome = { onFeedTabClick() },
            openProfile = { canisterData ->
                if (canisterData.userPrincipalId == sessionManager.userPrincipal) {
                    onProfileTabClick()
                } else {
                    openProfile(canisterData)
                }
            },
        )

    private fun tournamentComponent(componentContext: ComponentContext): TournamentComponent =
        TournamentComponent(
            componentContext = componentContext,
            promptLogin = { homeViewModel.showSignupPrompt(true, it) },
            navigateToLeaderboard = { tournamentId, participantsLabel, scheduleLabel ->
                openTournamentLeaderboard(tournamentId, participantsLabel, scheduleLabel)
            },
            navigateToTournament = { tournamentId, title, initialDiamonds, endEpochMs, totalPrizePool ->
                navigation.pushNew(
                    Config.TournamentGame(
                        tournamentId = tournamentId,
                        tournamentTitle = title,
                        initialDiamonds = initialDiamonds,
                        endEpochMs = endEpochMs,
                        totalPrizePool = totalPrizePool,
                    ),
                )
            },
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
            onLeaderboardClick = { _ ->
//                openTournamentLeaderboard()
            },
            onTimeUp = { navigation.pop() },
            onBack = { navigation.pop() },
        )

    private fun uploadVideoComponent(componentContext: ComponentContext): UploadVideoRootComponent =
        UploadVideoRootComponent.Companion(
            componentContext = componentContext,
            goToHome = {
                onFeedTabClick()
                showAlertsOnDialog(AlertsRequestType.VIDEO)
            },
            promptLogin = { homeViewModel.showSignupPrompt(true, it) },
            snapshot = childSnapshots[Config.UploadVideo] as? UploadVideoRootComponent.Snapshot,
        )

    private fun profileComponent(componentContext: ComponentContext): ProfileComponent =
        ProfileComponent.Companion(
            componentContext = componentContext,
            onUploadVideoClicked = { onUploadVideoTabClick() },
            openEditProfile = openEditProfile,
            openProfile = openProfile,
            snapshot = childSnapshots[Config.Profile] as? ProfileComponent.Snapshot,
            showAlertsOnDialog = showAlertsOnDialog,
            promptLogin = { homeViewModel.showSignupPrompt(true, it) },
        )

    private fun accountComponent(componentContext: ComponentContext): AccountComponent =
        AccountComponent.Companion(
            componentContext = componentContext,
            promptLogin = { homeViewModel.showSignupPrompt(true, it) },
        )

    private fun walletComponent(componentContext: ComponentContext): WalletComponent =
        WalletComponent.Companion(
            componentContext = componentContext,
            showAlertsOnDialog = showAlertsOnDialog,
        )

    private fun chatComponent(componentContext: ComponentContext): ChatComponent =
        ChatComponent.Companion(
            componentContext = componentContext,
            snapshot = childSnapshots[Config.Chat] as? ChatComponent.Snapshot,
        )

    private fun slotChild(
        config: SlotConfig,
        componentContext: ComponentContext,
    ): SlotChild =
        when (config) {
            is SlotConfig.VideoViewsRewardsBottomSheet ->
                SlotChild.VideoViewsRewardsBottomSheet(
                    component = btcRewardsComponent(componentContext),
                    data = config.data,
                )
        }

    private fun btcRewardsComponent(componentContext: ComponentContext): VideoViewRewardsComponent =
        DefaultVideoViewRewardsComponent(
            componentContext = componentContext,
            onDismissed = slotNavigation::dismiss,
            navigateToWallet = {
                slotNavigation.dismiss()
                onWalletTabClick()
            },
            navigateToFeed = {
                slotNavigation.dismiss()
                onFeedTabClick()
            },
        )

    private fun showSlot(slotConfig: SlotConfig) {
        slotNavigation.activate(slotConfig)
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Feed : Config

        @Serializable
        data object Leaderboard : Config

        @Serializable
        data object Tournament : Config

        @Serializable
        data class TournamentGame(
            val tournamentId: String,
            val tournamentTitle: String = "",
            val initialDiamonds: Int,
            val endEpochMs: Long,
            val totalPrizePool: Int,
        ) : Config

        @Serializable
        data object UploadVideo : Config

        @Serializable
        data object Profile : Config

        @Serializable
        data object Account : Config

        @Serializable
        data object Wallet : Config

        @Serializable
        data object Chat : Config
    }

    @Serializable
    private sealed interface SlotConfig {
        @Serializable
        data class VideoViewsRewardsBottomSheet(
            val data: RewardsReceived,
        ) : SlotConfig
    }
}
