package com.yral.shared.features.coach.nav

import com.arkivanov.decompose.ComponentContext

abstract class SoulFileComponent {
    abstract val openSoulFileParams: OpenSoulFileParams

    abstract fun onBack()

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            params: OpenSoulFileParams,
            onBack: () -> Unit,
        ): SoulFileComponent =
            DefaultSoulFileComponent(
                componentContext = componentContext,
                openSoulFileParams = params,
                onBack = onBack,
            )
    }
}
