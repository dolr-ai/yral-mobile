package com.yral.shared.data

import kotlinx.serialization.Serializable

@Serializable
enum class AlertsRequestType {
    FOLLOW_BACK,
    VIDEO,
    DEFAULT,
    TOURNAMENT,
}
