package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

internal class DefaultProfileMainComponent(
    componentContext: ComponentContext,
    override val pendingVideoNavigation: Flow<String?>,
    override val userCanisterData: CanisterData?,
    private val onUploadVideoClicked: () -> Unit,
    private val openAccount: () -> Unit,
    private val openEditProfile: () -> Unit,
    private val onBackClicked: () -> Unit,
) : ProfileMainComponent,
    ComponentContext by componentContext,
    KoinComponent {
    override fun onUploadVideoClick() {
        onUploadVideoClicked.invoke()
    }

    override fun openAccount() {
        openAccount.invoke()
    }

    override fun openEditProfile() {
        openEditProfile.invoke()
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }
}
