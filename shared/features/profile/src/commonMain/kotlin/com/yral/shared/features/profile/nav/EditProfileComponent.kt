package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext

abstract class EditProfileComponent {
    abstract fun onBack()

    companion object {
        operator fun invoke(
            componentContext: ComponentContext,
            onBack: () -> Unit,
        ): EditProfileComponent =
            DefaultEditProfileComponent(
                componentContext = componentContext,
                onBack = onBack,
            )
    }
}
