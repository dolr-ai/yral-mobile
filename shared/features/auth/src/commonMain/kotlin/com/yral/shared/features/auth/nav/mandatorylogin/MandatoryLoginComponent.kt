package com.yral.shared.features.auth.nav.mandatorylogin

import com.arkivanov.decompose.ComponentContext

abstract class MandatoryLoginComponent {
    abstract fun onNavigateToCountrySelector()
    abstract fun onNavigateToOtpVerification()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onNavigateToCountrySelector: () -> Unit,
            onNavigateToOtpVerification: () -> Unit,
        ): MandatoryLoginComponent =
            DefaultMandatoryLoginComponent(
                componentContext = componentContext,
                onNavigateToCountrySelector = onNavigateToCountrySelector,
                onNavigateToOtpVerification = onNavigateToOtpVerification,
            )
    }
}
