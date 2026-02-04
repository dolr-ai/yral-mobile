package com.yral.shared.app.ui.screens.home.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.featureflag.ChatFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.WalletFeatureFlags
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.app.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.account.nav.AccountComponent
import com.yral.shared.features.auth.ui.RequestLoginFactory
import com.yral.shared.features.chat.nav.ChatComponent
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.root.viewmodels.HomeViewModel
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.features.tournament.nav.TournamentComponent
import com.yral.shared.features.uploadvideo.nav.UploadVideoRootComponent
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.features.wallet.ui.btcRewards.nav.VideoViewRewardsComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.RewardsReceived
import com.yral.shared.rust.service.utils.CanisterData

abstract class HomeComponent {
    abstract val requestLoginFactory: RequestLoginFactory
    abstract val stack: Value<ChildStack<*, Child>>
    abstract val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    abstract val homeViewModel: HomeViewModel
    abstract val sessionManager: SessionManager
    abstract val subscriptionCoordinator: SubscriptionCoordinator

    abstract fun onFeedTabClick()
    abstract fun onLeaderboardTabClick()
    abstract fun onTournamentTabClick()
    abstract fun onUploadVideoTabClick()
    abstract fun onProfileTabClick()
    abstract fun onAccountTabClick()
    abstract fun onWalletTabClick()
    abstract fun onChatTabClick()
    abstract fun onNavigationRequest(appRoute: AppRoute)
    abstract fun openConversation(
        influencerId: String,
        influencerCategory: String = "",
        influencerSource: InfluencerSource = InfluencerSource.CARD,
    )
    abstract fun openWallet()
    abstract fun openLeaderboard()

    sealed class Child {
        class Feed(
            val component: FeedComponent,
        ) : Child()
        class Leaderboard(
            val component: LeaderboardComponent,
        ) : Child()
        class Tournament(
            val component: TournamentComponent,
        ) : Child()
        class UploadVideo(
            val component: UploadVideoRootComponent,
        ) : Child()
        class Profile(
            val component: ProfileComponent,
        ) : Child()
        class Account(
            val component: AccountComponent,
        ) : Child()
        class Wallet(
            val component: WalletComponent,
        ) : Child()
        class Chat(
            val component: ChatComponent,
        ) : Child()
    }

    abstract val slot: Value<ChildSlot<*, SlotChild>>

    sealed class SlotChild {
        class VideoViewsRewardsBottomSheet(
            val component: VideoViewRewardsComponent,
            val data: RewardsReceived,
        ) : SlotChild()
    }
    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            requestLoginFactory: RequestLoginFactory,
            subscriptionCoordinator: SubscriptionCoordinator,
            openEditProfile: () -> Unit,
            openProfile: (userCanisterData: CanisterData) -> Unit,
            openConversation: (
                influencerId: String,
                influencerCategory: String,
                influencerSource: InfluencerSource,
            ) -> Unit,
            openWallet: () -> Unit,
            openLeaderboard: () -> Unit,
            openTournamentLeaderboard: (
                tournamentId: String,
                showResult: Boolean,
            ) -> Unit,
            openTournamentGame: (
                tournamentId: String,
                tournamentTitle: String,
                initialDiamonds: Int,
                startEpochMs: Long,
                endEpochMs: Long,
                totalPrizePool: Int,
                isHotOrNot: Boolean,
            ) -> Unit,
            openAccountSheet: () -> Unit,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
        ): HomeComponent =
            DefaultHomeComponent(
                componentContext,
                requestLoginFactory,
                subscriptionCoordinator,
                openEditProfile,
                openProfile,
                openConversation,
                openWallet,
                openLeaderboard,
                openTournamentLeaderboard,
                openTournamentGame,
                openAccountSheet,
                showAlertsOnDialog,
            )
    }
}

internal fun FeatureFlagManager.getChatAndWalletConfig(): Pair<Boolean, Boolean> {
    val isWalletEnabled = isEnabled(WalletFeatureFlags.Wallet.Enabled)
    val isChatEnabled = isEnabled(ChatFeatureFlags.Chat.Enabled)
    // Show chat if enabled, otherwise show wallet if enabled
    // Chat takes precedence if both are enabled
    return isChatEnabled to (isWalletEnabled && !isChatEnabled)
}
