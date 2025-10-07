package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

internal class DefaultProfileMainComponent(
    componentContext: ComponentContext,
    override val pendingVideoNavigation: Flow<String?>,
    private val onUploadVideoClicked: () -> Unit,
    private val openAccount: () -> Unit,
) : ProfileMainComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun onUploadVideoClick() {
        onUploadVideoClicked.invoke()
    }

    override fun openAccount() {
        openAccount.invoke()
    }
}
