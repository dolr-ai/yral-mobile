package com.yral.android.ui.screens.uploadVideo

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.yral.android.ui.screens.uploadVideo.aiVideoGen.AiVideoGenComponent
import com.yral.android.ui.screens.uploadVideo.fileUpload.UploadVideoComponent
import com.yral.android.ui.screens.uploadVideo.flowSelection.FlowSelectionComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

internal class DefaultUploadVideoRootComponent(
    componentContext: ComponentContext,
    private val goToHome: () -> Unit,
    private val openAlertsRequestBottomSheet: () -> Unit,
) : UploadVideoRootComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.FlowSelection,
            handleBackButton = true,
            childFactory = ::child,
        )

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.FlowSelection -> Child.FlowSelection(flowSelectionComponent(componentContext))
            Config.AiVideoGen -> Child.AiVideoGen(aiVideoGenComponent(componentContext))
            Config.ClassicUpload -> Child.ClassicUpload(uploadVideoComponent(componentContext))
        }

    private fun flowSelectionComponent(componentContext: ComponentContext): FlowSelectionComponent =
        FlowSelectionComponent.Companion(
            componentContext = componentContext,
            onUploadVideoClicked = { navigation.replaceAll(Config.ClassicUpload) },
            onAiVideoGenClicked = { navigation.replaceAll(Config.AiVideoGen) },
        )

    private fun aiVideoGenComponent(componentContext: ComponentContext): AiVideoGenComponent =
        AiVideoGenComponent.Companion(
            componentContext = componentContext,
            onOpenAlertsRequest = openAlertsRequestBottomSheet,
        )

    private fun uploadVideoComponent(componentContext: ComponentContext): UploadVideoComponent =
        UploadVideoComponent.Companion(componentContext = componentContext, goToHome = goToHome)

    @Serializable
    private sealed interface Config {
        @Serializable
        data object FlowSelection : Config

        @Serializable
        data object AiVideoGen : Config

        @Serializable
        data object ClassicUpload : Config
    }
}
