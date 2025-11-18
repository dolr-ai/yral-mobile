package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

@Suppress("LongParameterList")
internal class DefaultProfileMainComponent(
    componentContext: ComponentContext,
    override val pendingVideoNavigation: Flow<String?>,
    override val userCanisterData: CanisterData?,
    private val onUploadVideoClicked: () -> Unit,
    private val openAccount: () -> Unit,
    private val openEditProfile: () -> Unit,
    private val openProfile: (CanisterData) -> Unit,
    private val onBackClicked: () -> Unit,
    override val showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
    override val promptLogin: () -> Unit,
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

    override fun openProfile(userCanisterData: CanisterData) {
        openProfile.invoke(userCanisterData)
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }
}
