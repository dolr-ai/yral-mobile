package com.yral.shared.features.account.nav

import com.arkivanov.decompose.ComponentContext

interface AccountComponent {
    val promptLogin: () -> Unit
    fun onBack()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit = {},
            promptLogin: () -> Unit,
        ): AccountComponent =
            DefaultAccountComponent(
                componentContext,
                onBack,
                promptLogin,
            )
    }
}
