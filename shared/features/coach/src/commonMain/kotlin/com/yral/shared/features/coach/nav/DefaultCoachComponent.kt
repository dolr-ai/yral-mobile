package com.yral.shared.features.coach.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultCoachComponent(
    componentContext: ComponentContext,
    override val openCoachParams: OpenCoachParams,
    private val onBack: () -> Unit,
) : CoachComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }
}
