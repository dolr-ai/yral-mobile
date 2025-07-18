package com.yral.android.ui.screens.alertsrequest.nav

import com.arkivanov.decompose.ComponentContext

interface AlertsRequestComponent {
    fun onDismissClicked()

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
