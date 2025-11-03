package com.yral.shared.app.ui.screens.home.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.app.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.account.nav.AccountComponent
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.uploadvideo.nav.UploadVideoRootComponent
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.features.wallet.ui.btcRewards.nav.VideoViewRewardsComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.RewardsReceived
import com.yral.shared.rust.service.utils.CanisterData

abstract class HomeComponent {
    abstract val stack: Value<ChildStack<*, Child>>
    abstract val showAlertsOnDialog: (type: AlertsRequestType) -> Unit

    abstract fun onFeedTabClick()
    abstract fun onLeaderboardTabClick()
    abstract fun onUploadVideoTabClick()
    abstract fun onProfileTabClick()
    abstract fun onAccountTabClick()
    abstract fun onWalletTabClick()
    abstract fun onNavigationRequest(appRoute: AppRoute)

    sealed class Child {
        class Feed(
            val component: FeedComponent,
        ) : Child()
        class Leaderboard(
            val component: LeaderboardComponent,
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
            openEditProfile: () -> Unit,
            openProfile: (userCanisterData: CanisterData) -> Unit,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
        ): HomeComponent = DefaultHomeComponent(componentContext, openEditProfile, openProfile, showAlertsOnDialog)
    }
}
