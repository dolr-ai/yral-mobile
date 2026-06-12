package com.yral.shared.features.coach.nav

import com.arkivanov.decompose.ComponentContext

abstract class CoachComponent {
    abstract val openCoachParams: OpenCoachParams

    abstract fun onBack()

    /**
     * Coach pivot Bucket 2 — open the read-only "View full prompt" page for
     * the same bot the current Coach session is targeting. Called from the
     * "View full prompt" pill in the Coach header.
     */
    abstract fun openSoulFile()

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            params: OpenCoachParams,
            onBack: () -> Unit,
            openSoulFile: (OpenSoulFileParams) -> Unit,
        ): CoachComponent =
            DefaultCoachComponent(
                componentContext = componentContext,
                openCoachParams = params,
                onBack = onBack,
                openSoulFile = openSoulFile,
            )
    }
}
