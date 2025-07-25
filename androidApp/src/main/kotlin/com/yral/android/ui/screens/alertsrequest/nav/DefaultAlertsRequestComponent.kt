package com.yral.android.ui.screens.alertsrequest.nav

import com.arkivanov.decompose.ComponentContext

internal class DefaultAlertsRequestComponent(
    componentContext: ComponentContext,
    private val onDismissed: () -> Unit,
) : AlertsRequestComponent,
    ComponentContext by componentContext {
    override fun onDismissClicked() {
        onDismissed()
    }
}
