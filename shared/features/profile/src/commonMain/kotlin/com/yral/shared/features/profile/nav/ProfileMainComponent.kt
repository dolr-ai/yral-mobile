package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow

interface ProfileMainComponent {
    val pendingVideoNavigation: Flow<String?>
    val userCanisterData: CanisterData?
    val showAlertsOnDialog: (type: AlertsRequestType) -> Unit
    val promptLogin: () -> Unit
    fun onUploadVideoClick()
    fun openAccount()
    fun openEditProfile()
    fun openProfile(userCanisterData: CanisterData)
    fun onBackClicked()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            userCanisterData: CanisterData? = null,
            pendingVideoNavigation: Flow<String?>,
            onUploadVideoClicked: () -> Unit,
            openAccount: () -> Unit,
            openEditProfile: () -> Unit,
            openProfile: (CanisterData) -> Unit,
            onBackClicked: () -> Unit,
            showAlertsOnDialog: (type: AlertsRequestType) -> Unit,
            promptLogin: () -> Unit,
        ): ProfileMainComponent =
            DefaultProfileMainComponent(
                componentContext = componentContext,
                userCanisterData = userCanisterData,
                pendingVideoNavigation = pendingVideoNavigation,
                onUploadVideoClicked = onUploadVideoClicked,
                openAccount = openAccount,
                openEditProfile = openEditProfile,
                openProfile = openProfile,
                onBackClicked = onBackClicked,
                showAlertsOnDialog = showAlertsOnDialog,
                promptLogin = promptLogin,
            )
    }
}
