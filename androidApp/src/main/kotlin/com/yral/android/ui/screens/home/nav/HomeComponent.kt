package com.yral.android.ui.screens.home.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.account.nav.AccountComponent
import com.yral.android.ui.screens.feed.nav.FeedComponent
import com.yral.android.ui.screens.leaderboard.nav.LeaderboardComponent
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.android.ui.screens.uploadVideo.nav.UploadVideoComponent

abstract class HomeComponent {
    abstract val stack: Value<ChildStack<*, Child>>

    abstract fun onFeedTabClick()
    abstract fun onLeaderboardTabClick()
    abstract fun onUploadVideoTabClick()
    abstract fun onProfileTabClick()
    abstract fun onAccountTabClick()

    sealed class Child {
        class Feed(
            val component: FeedComponent,
        ) : Child()
        class Leaderboard(
            val component: LeaderboardComponent,
        ) : Child()
        class UploadVideo(
            val component: UploadVideoComponent,
        ) : Child()
        class Profile(
            val component: ProfileComponent,
        ) : Child()
        class Account(
            val component: AccountComponent,
        ) : Child()
    }

    companion object {
        operator fun invoke(componentContext: ComponentContext): HomeComponent = DefaultHomeComponent(componentContext)
    }
}
