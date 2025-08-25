package com.yral.android.ui.screens.uploadVideo.aiVideoGen

import com.arkivanov.decompose.ComponentContext

internal class DefaultAiVideoGenComponent(
    componentContext: ComponentContext,
    private val goToHome: () -> Unit,
    private val onBack: () -> Unit,
) : AiVideoGenComponent(),
    ComponentContext by componentContext {
    override fun onBack() {
        onBack.invoke()
    }

    override fun goToHome() {
        goToHome.invoke()
    }
}
