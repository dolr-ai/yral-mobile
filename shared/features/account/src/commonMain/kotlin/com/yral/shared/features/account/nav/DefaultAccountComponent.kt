package com.yral.shared.features.account.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultAccountComponent(
    componentContext: ComponentContext,
    val onBack: () -> Unit = {},
    override val promptLogin: () -> Unit,
) : AccountComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }
}
