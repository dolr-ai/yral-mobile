package com.yral.android.ui.screens.uploadVideo

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
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
    private val snapshot: Snapshot?,
) : UploadVideoRootComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialStack = {
                val saved = snapshot?.routes ?: emptyList()
                if (saved.isEmpty()) listOf(Config.FlowSelection) else saved.map { it.toConfig() }
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
                        is Config.FlowSelection -> Snapshot.Route.FlowSelection
                        is Config.AiVideoGen -> Snapshot.Route.AiVideoGen
                        is Config.FileUpload -> Snapshot.Route.FileUpload
                        else -> Snapshot.Route.FlowSelection
                    }
                },
        )

    private fun Snapshot.Route.toConfig(): Config =
        when (this) {
            Snapshot.Route.FlowSelection -> Config.FlowSelection
            Snapshot.Route.AiVideoGen -> Config.AiVideoGen
            Snapshot.Route.FileUpload -> Config.FileUpload
        }

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.FlowSelection -> Child.FlowSelection(flowSelectionComponent(componentContext))
            Config.AiVideoGen -> Child.AiVideoGen(aiVideoGenComponent(componentContext))
            Config.FileUpload -> Child.FileUpload(uploadVideoComponent(componentContext))
        }

    @OptIn(DelicateDecomposeApi::class)
    private fun flowSelectionComponent(componentContext: ComponentContext): FlowSelectionComponent =
        FlowSelectionComponent.Companion(
            componentContext = componentContext,
            onUploadVideoClicked = { navigation.push(Config.FileUpload) },
            onAiVideoGenClicked = { navigation.push(Config.AiVideoGen) },
        )

    private fun aiVideoGenComponent(componentContext: ComponentContext): AiVideoGenComponent =
        AiVideoGenComponent.Companion(
            componentContext = componentContext,
            onOpenAlertsRequest = openAlertsRequestBottomSheet,
        )

    private fun uploadVideoComponent(componentContext: ComponentContext): UploadVideoComponent =
        UploadVideoComponent.Companion(
            componentContext = componentContext,
            goToHome =
                {
                    navigation.replaceAll(Config.FlowSelection)
                    goToHome.invoke()
                },
            onBack = { navigation.pop() },
        )

    @Serializable
    private sealed interface Config {
        @Serializable
        data object FlowSelection : Config

        @Serializable
        data object AiVideoGen : Config

        @Serializable
        data object FileUpload : Config
    }
}
