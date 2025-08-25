package com.yral.android.ui.screens.uploadVideo.flowSelection

import com.arkivanov.decompose.ComponentContext

abstract class FlowSelectionComponent {
    abstract fun onUploadVideoClicked()
    abstract fun onAiVideoGenClicked()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onUploadVideoClicked: () -> Unit,
            onAiVideoGenClicked: () -> Unit,
        ): FlowSelectionComponent =
            DefaultFlowSelectionComponent(
                componentContext = componentContext,
                onClassicUploadClicked = onUploadVideoClicked,
                onAiVideoGenClicked = onAiVideoGenClicked,
            )
    }
}
