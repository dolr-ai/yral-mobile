package com.yral.android.ui.screens.home.nav

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
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.account.nav.AccountComponent
import com.yral.android.ui.screens.alertsrequest.nav.AlertsRequestComponent
import com.yral.android.ui.screens.feed.nav.FeedComponent
import com.yral.android.ui.screens.leaderboard.nav.LeaderboardComponent
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.android.ui.screens.uploadVideo.nav.UploadVideoComponent
import kotlinx.serialization.Serializable

internal class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val navigateToAccount: () -> Unit,
) : HomeComponent(),
    ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Feed,
            handleBackButton = true,
            childFactory = ::child,
        )

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

    override fun onUploadVideoTabClick() {
        navigation.replaceKeepingFeed(Config.UploadVideo)
    }

    override fun onProfileTabClick() {
        navigation.replaceKeepingFeed(Config.Profile)
    }

    override fun onAccountTabClick() {
        navigation.replaceKeepingFeed(Config.Account)
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
            is Config.UploadVideo -> Child.UploadVideo(uploadVideoComponent(componentContext))
            is Config.Profile -> Child.Profile(profileComponent(componentContext))
            is Config.Account -> Child.Account(accountComponent(componentContext))
        }

    private fun feedComponent(componentContext: ComponentContext): FeedComponent =
        FeedComponent.Companion(
            componentContext = componentContext,
            navigateToAccount = navigateToAccount,
        )

    private fun leaderboardComponent(componentContext: ComponentContext): LeaderboardComponent =
        LeaderboardComponent.Companion(componentContext = componentContext)

    private fun uploadVideoComponent(componentContext: ComponentContext): UploadVideoComponent =
        UploadVideoComponent.Companion(
            componentContext = componentContext,
            goToHome = {
                onFeedTabClick()
                showSlot(SlotConfig.AlertsRequestBottomSheet)
            },
        )

    private fun profileComponent(componentContext: ComponentContext): ProfileComponent =
        ProfileComponent.Companion(
            componentContext = componentContext,
            onUploadVideoClicked = { onUploadVideoTabClick() },
        )

    private fun accountComponent(componentContext: ComponentContext): AccountComponent =
        AccountComponent.Companion(componentContext = componentContext)

    private fun slotChild(
        config: SlotConfig,
        componentContext: ComponentContext,
    ): SlotChild =
        when (config) {
            SlotConfig.AlertsRequestBottomSheet ->
                SlotChild.AlertsRequestBottomSheet(
                    alertsRequestComponent(componentContext),
                )
        }

    private fun alertsRequestComponent(componentContext: ComponentContext): AlertsRequestComponent =
        AlertsRequestComponent(
            componentContext = componentContext,
            onDismissed = slotNavigation::dismiss,
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
        data object UploadVideo : Config

        @Serializable
        data object Profile : Config

        @Serializable
        data object Account : Config
    }

    @Serializable
    private sealed interface SlotConfig {
        @Serializable
        data object AlertsRequestBottomSheet : SlotConfig
    }
}
