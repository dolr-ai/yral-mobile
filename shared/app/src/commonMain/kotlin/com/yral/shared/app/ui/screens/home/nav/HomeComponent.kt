package com.yral.shared.app.ui.screens.home.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.app.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.features.account.nav.AccountComponent
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.features.root.viewmodels.HomeViewModel
import com.yral.shared.features.uploadvideo.nav.UploadVideoRootComponent
import com.yral.shared.features.wallet.nav.WalletComponent
import com.yral.shared.features.wallet.ui.btcRewards.nav.VideoViewRewardsComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.RewardsReceived
import com.yral.shared.rust.service.utils.CanisterData

abstract class HomeComponent {
    abstract val stack: Value<ChildStack<*, Child>>
    abstract val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    abstract val homeViewModel: HomeViewModel
    abstract val sessionManager: SessionManager

    abstract fun onFeedTabClick()
    abstract fun onLeaderboardTabClick()
    abstract fun onUploadVideoTabClick()
    abstract fun onProfileTabClick()
    abstract fun onAccountTabClick()
    abstract fun onWalletTabClick()
    abstract fun onNavigationRequest(appRoute: AppRoute)
    abstract fun showLoginBottomSheet(
        pageName: SignupPageName,
        loginBottomSheetType: LoginBottomSheetType,
        onDismissRequest: () -> Unit,
        onLoginSuccess: () -> Unit = {},
    )
    abstract fun hideLoginBottomSheetIfVisible()

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
            showLoginBottomSheet: (
                pageName: SignupPageName,
                loginBottomSheetType: LoginBottomSheetType,
                onDismissRequest: () -> Unit,
                onLoginSuccess: () -> Unit,
            ) -> Unit,
            hideLoginBottomSheetIfVisible: () -> Unit,
        ): HomeComponent =
            DefaultHomeComponent(
                componentContext,
                openEditProfile,
                openProfile,
                showAlertsOnDialog,
                showLoginBottomSheet,
                hideLoginBottomSheetIfVisible,
            )
    }
}
