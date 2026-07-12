package com.yral.shared.features.coach.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultSoulFileComponent(
    componentContext: ComponentContext,
    override val openSoulFileParams: OpenSoulFileParams,
    private val onBack: () -> Unit,
) : SoulFileComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }
}
