package com.yral.shared.features.account.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SignupPageName

interface AccountComponent {
    val promptLogin: (pageName: SignupPageName) -> Unit
    fun onBack()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit = {},
            promptLogin: (pageName: SignupPageName) -> Unit,
        ): AccountComponent =
            DefaultAccountComponent(
                componentContext,
                onBack,
                promptLogin,
            )
    }
}
