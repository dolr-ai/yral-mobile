package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultEditProfileComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : EditProfileComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }
}
