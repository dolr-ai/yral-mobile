package com.yral.shared.app.ui.screens.alertsrequest.nav

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.StateFlow

interface AlertsRequestComponent {
    val showSheet: StateFlow<Boolean>
    fun onDismissClicked()
    fun onPermissionChanged(isGranted: Boolean)

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onDismissed: () -> Unit,
        ): AlertsRequestComponent =
            DefaultAlertsRequestComponent(
                componentContext,
                onDismissed,
            )
    }
}
