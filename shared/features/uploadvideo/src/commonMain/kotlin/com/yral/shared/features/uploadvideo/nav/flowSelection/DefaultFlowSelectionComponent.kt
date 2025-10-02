package com.yral.shared.features.uploadvideo.nav.flowSelection

import com.arkivanov.decompose.ComponentContext

internal class DefaultFlowSelectionComponent(
    componentContext: ComponentContext,
    private val onClassicUploadClicked: () -> Unit,
    private val onAiVideoGenClicked: () -> Unit,
) : FlowSelectionComponent(),
    ComponentContext by componentContext {
    override fun onUploadVideoClicked() {
        onClassicUploadClicked.invoke()
    }

    override fun onAiVideoGenClicked() {
        onAiVideoGenClicked.invoke()
    }
}
