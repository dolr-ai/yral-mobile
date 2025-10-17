package com.yral.shared.features.profile.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.flow.Flow

interface ProfileMainComponent {
    val pendingVideoNavigation: Flow<String?>
    val userCanisterData: CanisterData?
    fun onUploadVideoClick()
    fun openAccount()
    fun openEditProfile()
    fun onBackClicked()
    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            userCanisterData: CanisterData? = null,
            pendingVideoNavigation: Flow<String?>,
            onUploadVideoClicked: () -> Unit,
            openAccount: () -> Unit,
            openEditProfile: () -> Unit,
            onBackClicked: () -> Unit,
        ): ProfileMainComponent =
            DefaultProfileMainComponent(
                componentContext = componentContext,
                userCanisterData = userCanisterData,
                pendingVideoNavigation = pendingVideoNavigation,
                onUploadVideoClicked = onUploadVideoClicked,
                openAccount = openAccount,
                openEditProfile = openEditProfile,
                onBackClicked = onBackClicked,
            )
    }
}
