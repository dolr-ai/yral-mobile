package com.yral.shared.app.ui.screens.profile.nav

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.account.nav.AccountComponent
import com.yral.shared.features.profile.nav.EditProfileComponent
import com.yral.shared.features.profile.nav.ProfileMainComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.VideoUploadSuccessful
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

internal class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val snapshot: Snapshot?,
    private val onUploadVideoClicked: () -> Unit,
    private val openEditProfile: () -> Unit,
    private val openProfile: (CanisterData) -> Unit,
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

    override fun onNavigationRequest(appRoute: AppRoute) {
        Logger.d("DefaultProfileComponent") { "handleNavigation: $appRoute" }
        when (appRoute) {
            is VideoUploadSuccessful -> {
                appRoute.videoID?.let { videoId ->
                    val channelResult = _pendingVideoNavigation.trySend(videoId)
                    Logger.d("DefaultProfileComponent") { "handleNavigation: channelResult: $channelResult" }
                }
            }
            else -> Unit
        }
    }

    override fun openAccount() {
        navigation.pushToFront(Config.Account)
    }

    override fun openEditProfile() {
        openEditProfile.invoke()
        // navigation.pushToFront(Config.EditProfile)
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

    override fun createHomeSnapshot(): Snapshot {
        val routes =
            stack.value.items.map { item ->
                return@map when (val config = item.configuration) {
                    is Config.Main -> ProfileComponent.Snapshot.Route.Main
                    is Config.Account -> ProfileComponent.Snapshot.Route.Account
                    is Config.EditProfile -> ProfileComponent.Snapshot.Route.EditProfile
                    else -> error("Unsupported profile config: $config")
                }
            }
        return Snapshot(routes = routes)
    }

    private fun Snapshot.Route.toConfig(): Config =
        when (this) {
            Snapshot.Route.Main -> Config.Main
            Snapshot.Route.Account -> Config.Account
            Snapshot.Route.EditProfile -> Config.EditProfile
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.Main -> Child.Main(profileMainComponent(componentContext))
            Config.Account -> Child.Account(accountComponent(componentContext))
            Config.EditProfile -> Child.EditProfile(editProfileComponent(componentContext))
        }

    private fun profileMainComponent(componentContext: ComponentContext): ProfileMainComponent =
        ProfileMainComponent.Companion(
            componentContext = componentContext,
            pendingVideoNavigation = pendingVideoNavigation,
            onUploadVideoClicked = onUploadVideoClicked,
            openAccount = this::openAccount,
            openEditProfile = this::openEditProfile,
            openProfile = openProfile,
            onBackClicked = {},
        )

    private fun accountComponent(componentContext: ComponentContext): AccountComponent =
        AccountComponent.Companion(
            componentContext = componentContext,
            onBack = this::onBackClicked,
        )

    private fun editProfileComponent(componentContext: ComponentContext): EditProfileComponent =
        EditProfileComponent.Companion(
            componentContext = componentContext,
            onBack = this::onBackClicked,
        )

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Main : Config

        @Serializable
        data object Account : Config

        @Serializable
        data object EditProfile : Config
    }
}
