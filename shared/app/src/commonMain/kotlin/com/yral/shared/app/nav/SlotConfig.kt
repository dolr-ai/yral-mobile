package com.yral.shared.app.nav

import com.yral.shared.data.AlertsRequestType
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface SlotConfig {
    @Serializable
    data class AlertsRequestBottomSheet(
        val requestType: AlertsRequestType,
    ) : SlotConfig

    @Serializable
    data object LoginBottomSheet : SlotConfig

    @Serializable
    data object SubscriptionAccountMismatchSheet : SlotConfig

    @Serializable
    data object SubscriptionNudge : SlotConfig
}
