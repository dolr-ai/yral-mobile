package com.yral.android.ui.screens.profile.nav

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.account.nav.AccountComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

internal class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val snapshot: Snapshot?,
    private val onUploadVideoClicked: () -> Unit,
) : ProfileComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<Config>()

    private val _pendingVideoNavigation = Channel<String?>(Channel.CONFLATED)
    override val pendingVideoNavigation: Flow<String?> = _pendingVideoNavigation.receiveAsFlow()

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialStack = {
                val saved = snapshot?.routes ?: emptyList()
                if (saved.isEmpty()) listOf(Config.Main) else saved.map { it.toConfig() }
            },
            handleBackButton = true,
            childFactory = ::child,
        )

    override fun onUploadVideoClick() {
        onUploadVideoClicked.invoke()
    }

    override fun handleNavigation(destination: String) {
        Logger.d("DefaultProfileComponent") { "handleNavigation: $destination" }
        when {
            destination.startsWith(DEEPLINK_VIDEO_PREFIX) -> {
                val videoId = destination.substringAfterLast("/videos/")
                val channelResult = _pendingVideoNavigation.trySend(videoId)
                Logger.d("DefaultProfileComponent") { "handleNavigation: channelResult: $channelResult" }
            }
        }
    }

    override fun openAccount() {
        navigation.push(Config.Account)
    }

    override fun onBackClicked(): Boolean {
        val items = stack.value.items
        return if (items.size > 1) {
            navigation.pop()
            true
        } else {
            false
        }
    }

    override fun createHomeSnapshot(): Snapshot =
        Snapshot(
            routes =
                stack.value.items.map { item ->
                    when (item.configuration) {
                        is Config.Main -> Snapshot.Route.Main
                        is Config.Account -> Snapshot.Route.Account
                        else -> Snapshot.Route.Main
                    }
                },
        )

    private fun Snapshot.Route.toConfig(): Config =
        when (this) {
            Snapshot.Route.Main -> Config.Main
            Snapshot.Route.Account -> Config.Account
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.Main -> Child.Main(profileMainComponent(componentContext))
            Config.Account -> Child.Account(accountComponent(componentContext))
        }

    private fun profileMainComponent(componentContext: ComponentContext): ProfileMainComponent =
        ProfileMainComponent.Companion(
            componentContext = componentContext,
            pendingVideoNavigation = pendingVideoNavigation,
            onUploadVideoClicked = onUploadVideoClicked,
            openAccount = this::openAccount,
        )

    private fun accountComponent(componentContext: ComponentContext): AccountComponent =
        AccountComponent.Companion(
            componentContext = componentContext,
            onBack = this::onBackClicked,
        )

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Main : Config

        @Serializable
        data object Account : Config
    }
}
