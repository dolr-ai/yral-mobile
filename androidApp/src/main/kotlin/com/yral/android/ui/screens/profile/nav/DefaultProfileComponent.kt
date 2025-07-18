package com.yral.android.ui.screens.profile.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val onUploadVideoClicked: () -> Unit,
) : ProfileComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onUploadVideoClick() {
        onUploadVideoClicked.invoke()
    }
}
