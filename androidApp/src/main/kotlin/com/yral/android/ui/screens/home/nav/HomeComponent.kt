package com.yral.android.ui.screens.home.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.account.nav.AccountComponent
import com.yral.android.ui.screens.alertsrequest.nav.AlertsRequestComponent
import com.yral.android.ui.screens.feed.nav.FeedComponent
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.android.ui.screens.uploadVideo.UploadVideoRootComponent
import com.yral.android.ui.screens.wallet.nav.WalletComponent
import com.yral.shared.features.leaderboard.nav.LeaderboardComponent
import com.yral.shared.libs.routing.routes.api.AppRoute

abstract class HomeComponent {
    abstract val stack: Value<ChildStack<*, Child>>

    abstract fun onFeedTabClick()
    abstract fun onLeaderboardTabClick()
    abstract fun onUploadVideoTabClick()
    abstract fun onProfileTabClick()
    abstract fun onAccountTabClick()
    abstract fun onWalletTabClick()

    @Deprecated("use onNavigationRequest")
    abstract fun handleNavigation(destination: String)

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
        class AlertsRequestBottomSheet(
            val component: AlertsRequestComponent,
        ) : SlotChild()
    }
    companion object {
        operator fun invoke(componentContext: ComponentContext): HomeComponent = DefaultHomeComponent(componentContext)
    }
}
