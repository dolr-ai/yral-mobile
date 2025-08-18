package com.yral.android.ui.screens.uploadVideo.aiVideoGen

import com.arkivanov.decompose.ComponentContext

internal class DefaultAiVideoGenComponent(
    componentContext: ComponentContext,
    private val onOpenAlertsRequest: () -> Unit,
    private val onBack: () -> Unit,
) : AiVideoGenComponent(),
    ComponentContext by componentContext {
    override fun onOpenAlertsRequest() {
        onOpenAlertsRequest.invoke()
    }

    override fun onBack() {
        onBack.invoke()
    }
}
