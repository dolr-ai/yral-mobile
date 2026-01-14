package com.yral.shared.features.auth.nav.mandatorylogin

import com.arkivanov.decompose.ComponentContext

internal class DefaultMandatoryLoginComponent(
    componentContext: ComponentContext,
    private val onNavigateToCountrySelector: () -> Unit,
    private val onNavigateToOtpVerification: () -> Unit,
) : MandatoryLoginComponent(),
    ComponentContext by componentContext {
    override fun onNavigateToCountrySelector() {
        onNavigateToCountrySelector.invoke()
    }

    override fun onNavigateToOtpVerification() {
        onNavigateToOtpVerification.invoke()
    }
}
