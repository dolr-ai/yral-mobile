package com.yral.android.ui.screens.uploadVideo.aiVideoGen

import com.arkivanov.decompose.ComponentContext

abstract class AiVideoGenComponent {
    abstract fun onOpenAlertsRequest()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onOpenAlertsRequest: () -> Unit,
        ): AiVideoGenComponent = DefaultAiVideoGenComponent(componentContext, onOpenAlertsRequest)
    }
}
