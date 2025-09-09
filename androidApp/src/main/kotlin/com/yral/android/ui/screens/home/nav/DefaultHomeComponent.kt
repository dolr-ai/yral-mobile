package com.yral.android.ui.screens.home.nav

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
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.account.nav.AccountComponent
import com.yral.android.ui.screens.alertsrequest.nav.AlertsRequestComponent
import com.yral.android.ui.screens.feed.nav.FeedComponent
import com.yral.android.ui.screens.leaderboard.nav.LeaderboardComponent
import com.yral.android.ui.screens.profile.nav.ProfileComponent
import com.yral.android.ui.screens.uploadVideo.UploadVideoRootComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.PostDetailsRoute
import kotlinx.serialization.Serializable

internal class DefaultHomeComponent(
    componentContext: ComponentContext,
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

    @Deprecated("use onNavigationRequest")
    override fun handleNavigation(destination: String) {
        Logger.d("DefaultHomeComponent") { "handleNavigation: $destination" }
        when {
            destination.startsWith(ProfileComponent.DEEPLINK) -> {
                navigation.replaceKeepingFeed(Config.Profile) {
                    (stack.value.active.instance as? Child.Profile)?.component?.handleNavigation(destination)
                }
            }
        }
    }

    override fun onNavigationRequest(appRoute: AppRoute) {
        when(appRoute) {
            is PostDetailsRoute -> navigation.replaceAll(Config.Feed) {
                (stack.value.active.instance as? Child.Feed)?.component?.openPostDetails(appRoute)
            }
            else -> {}
        }
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

    private fun mapActiveChild(child: Child): Pair<Config, HomeChildSnapshotProvider?> =
        when (child) {
            is Child.Feed -> Config.Feed to (child.component as? HomeChildSnapshotProvider)
            is Child.Leaderboard -> Config.Leaderboard to (child.component as? HomeChildSnapshotProvider)
            is Child.UploadVideo -> Config.UploadVideo to child.component
            is Child.Profile -> Config.Profile to (child.component as? HomeChildSnapshotProvider)
            is Child.Account -> Config.Account to (child.component as? HomeChildSnapshotProvider)
        }

    private fun feedComponent(componentContext: ComponentContext): FeedComponent =
        FeedComponent.Companion(componentContext = componentContext)

    private fun leaderboardComponent(componentContext: ComponentContext): LeaderboardComponent =
        LeaderboardComponent.Companion(componentContext = componentContext)

    private fun uploadVideoComponent(componentContext: ComponentContext): UploadVideoRootComponent =
        UploadVideoRootComponent.Companion(
            componentContext = componentContext,
            goToHome = {
                onFeedTabClick()
                showSlot(SlotConfig.AlertsRequestBottomSheet)
            },
            snapshot = childSnapshots[Config.UploadVideo] as? UploadVideoRootComponent.Snapshot,
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
