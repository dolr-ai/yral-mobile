package com.yral.shared.features.auth.nav.otpverification

import com.arkivanov.decompose.ComponentContext

internal class DefaultOtpVerificationComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : OtpVerificationComponent(),
    ComponentContext by componentContext {
    override fun onBack() {
        onBack.invoke()
    }
}
