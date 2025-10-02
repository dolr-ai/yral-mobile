package com.yral.shared.features.uploadvideo.nav.aiVideoGen

import com.arkivanov.decompose.ComponentContext

abstract class AiVideoGenComponent {
    abstract fun onBack()

    abstract fun goToHome()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            goToHome: () -> Unit,
            onBack: () -> Unit,
        ): AiVideoGenComponent =
            DefaultAiVideoGenComponent(
                componentContext,
                goToHome,
                onBack,
            )
    }
}
