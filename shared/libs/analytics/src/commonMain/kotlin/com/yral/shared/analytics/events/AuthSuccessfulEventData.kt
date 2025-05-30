package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import com.yral.shared.analytics.constants.Features
import kotlinx.serialization.Serializable

@Serializable
data class AuthSuccessfulEventData(
    override val event: String = FeatureEvents.AUTH_SUCCESSFUL.getEventName(),
    override val featureName: String = Features.AUTH.getFeatureName(),
) : EventData
