package com.yral.shared.app.ui.screens.alertsrequest.nav

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.data.AlertsRequestType
import kotlinx.coroutines.flow.StateFlow

interface AlertsRequestComponent {
    val type: AlertsRequestType
    val showSheet: StateFlow<Boolean>
    fun onDismissClicked()
    fun onPermissionChanged(isGranted: Boolean)

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            type: AlertsRequestType,
            onDismissed: () -> Unit,
        ): AlertsRequestComponent =
            DefaultAlertsRequestComponent(
                componentContext,
                type,
                onDismissed,
            )
    }
}
