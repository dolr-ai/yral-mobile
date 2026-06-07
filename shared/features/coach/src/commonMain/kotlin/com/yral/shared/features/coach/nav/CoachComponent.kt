package com.yral.shared.features.coach.nav

import com.arkivanov.decompose.ComponentContext

abstract class CoachComponent {
    abstract val openCoachParams: OpenCoachParams

    abstract fun onBack()

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            params: OpenCoachParams,
            onBack: () -> Unit,
        ): CoachComponent =
            DefaultCoachComponent(
                componentContext = componentContext,
                openCoachParams = params,
                onBack = onBack,
            )
    }
}
