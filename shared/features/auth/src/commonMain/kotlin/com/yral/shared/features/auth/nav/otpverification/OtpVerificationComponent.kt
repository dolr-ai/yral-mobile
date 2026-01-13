package com.yral.shared.features.auth.nav.otpverification

import com.arkivanov.decompose.ComponentContext

abstract class OtpVerificationComponent {
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit,
        ): OtpVerificationComponent =
            DefaultOtpVerificationComponent(
                componentContext = componentContext,
                onBack = onBack,
            )
    }
}
