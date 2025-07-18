package com.yral.android.ui.screens.profile.nav

import com.arkivanov.decompose.ComponentContext

abstract class ProfileComponent {
    abstract fun onUploadVideoClick()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onUploadVideoClicked: () -> Unit,
        ): ProfileComponent = DefaultProfileComponent(componentContext, onUploadVideoClicked)
    }
}
