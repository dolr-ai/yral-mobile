package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow

interface ProfileMainComponent {
    val pendingVideoNavigation: Flow<String?>
    fun onUploadVideoClick()
    fun openAccount()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            pendingVideoNavigation: Flow<String?>,
            onUploadVideoClicked: () -> Unit,
            openAccount: () -> Unit,
        ): ProfileMainComponent =
            DefaultProfileMainComponent(
                componentContext = componentContext,
                pendingVideoNavigation = pendingVideoNavigation,
                onUploadVideoClicked = onUploadVideoClicked,
                openAccount = openAccount,
            )
    }
}
