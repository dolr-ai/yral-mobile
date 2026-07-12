package com.yral.shared.features.uploadvideo.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.subscriptions.nav.SubscriptionCoordinator
import com.yral.shared.features.uploadvideo.nav.aiVideoGen.AiVideoGenComponent
import com.yral.shared.features.uploadvideo.nav.fileUpload.UploadVideoComponent
import com.yral.shared.libs.routing.routes.api.AppRoute
import com.yral.shared.libs.routing.routes.api.GenerateAIVideo
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

internal class DefaultUploadVideoRootComponent(
    componentContext: ComponentContext,
    override val promptLogin: (pageName: SignupPageName) -> Unit,
    private val goToHome: () -> Unit,
    override val subscriptionCoordinator: SubscriptionCoordinator,
    private val snapshot: Snapshot?,
    private val goToProfile: () -> Unit = {},
) : UploadVideoRootComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<Config>()
    private val iGoToHome =
        {
            navigation.replaceAll(Config.AiVideoGen)
            goToHome.invoke()
        }

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialStack = {
                val saved = snapshot?.routes ?: emptyList()
                if (saved.isEmpty()) listOf(Config.AiVideoGen) else saved.map { it.toConfig() }
            },
            handleBackButton = true,
            childFactory = ::child,
        )

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
                        is Config.AiVideoGen -> Snapshot.Route.AiVideoGen
                        is Config.FileUpload -> Snapshot.Route.FileUpload
                        else -> Snapshot.Route.AiVideoGen
                    }
                },
        )

    private fun Snapshot.Route.toConfig(): Config =
        when (this) {
            Snapshot.Route.FlowSelection -> Config.AiVideoGen
            Snapshot.Route.AiVideoGen -> Config.AiVideoGen
            Snapshot.Route.FileUpload -> Config.FileUpload
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.AiVideoGen -> Child.AiVideoGen(aiVideoGenComponent(componentContext))
            Config.FileUpload -> Child.FileUpload(uploadVideoComponent(componentContext))
        }

    private fun aiVideoGenComponent(componentContext: ComponentContext): AiVideoGenComponent =
        AiVideoGenComponent.Companion(
            componentContext = componentContext,
            onBack = {
                if (stack.value.items.size > 1) {
                    navigation.pop()
                } else {
                    iGoToHome()
                }
            },
            goToHome = iGoToHome,
            promptLogin = { promptLogin(SignupPageName.VIDEO_CREATION) },
            subscriptionCoordinator = subscriptionCoordinator,
            goToProfile = {
                navigation.replaceAll(Config.AiVideoGen)
                goToProfile()
            },
        )

    private fun uploadVideoComponent(componentContext: ComponentContext): UploadVideoComponent =
        UploadVideoComponent.Companion(
            componentContext = componentContext,
            goToHome = iGoToHome,
            onBack = { navigation.pop() },
            promptLogin = { promptLogin(SignupPageName.UPLOAD_VIDEO) },
        )

    override fun handleNavigation(appRoute: AppRoute) {
        when (appRoute) {
            is GenerateAIVideo -> {
                navigation.pushToFront(Config.AiVideoGen)
            }

            else -> {}
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object AiVideoGen : Config

        @Serializable
        data object FileUpload : Config
    }
}
