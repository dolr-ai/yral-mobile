package com.yral.android.ui.screens.uploadVideo.aiVideoGen

import com.arkivanov.decompose.ComponentContext

internal class DefaultAiVideoGenComponent(
    componentContext: ComponentContext,
    private val onOpenAlertsRequest: () -> Unit,
) : AiVideoGenComponent(),
    ComponentContext by componentContext {
    override fun onOpenAlertsRequest() {
        onOpenAlertsRequest.invoke()
    }
}
