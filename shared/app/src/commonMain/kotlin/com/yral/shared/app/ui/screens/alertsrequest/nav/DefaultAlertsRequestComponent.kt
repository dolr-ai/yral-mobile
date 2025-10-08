package com.yral.shared.app.ui.screens.alertsrequest.nav

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DefaultAlertsRequestComponent(
    componentContext: ComponentContext,
    private val onDismissed: () -> Unit,
) : AlertsRequestComponent,
    ComponentContext by componentContext {
    private val _showSheet = MutableStateFlow(false)
    override val showSheet: StateFlow<Boolean> = _showSheet.asStateFlow()

    override fun onDismissClicked() {
        onDismissed()
    }

    override fun onPermissionChanged(isGranted: Boolean) {
        if (isGranted) {
            onDismissed()
        }
        _showSheet.value = !isGranted
    }
}
