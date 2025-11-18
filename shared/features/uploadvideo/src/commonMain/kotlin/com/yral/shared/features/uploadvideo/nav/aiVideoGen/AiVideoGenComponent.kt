package com.yral.shared.features.uploadvideo.nav.aiVideoGen

import com.arkivanov.decompose.ComponentContext

abstract class AiVideoGenComponent {
    abstract val promptLogin: () -> Unit
    abstract fun onBack()

    abstract fun goToHome()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            goToHome: () -> Unit,
            onBack: () -> Unit,
            promptLogin: () -> Unit,
        ): AiVideoGenComponent =
            DefaultAiVideoGenComponent(
                componentContext,
                goToHome,
                onBack,
                promptLogin,
            )
    }
}
