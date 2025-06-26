package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import com.yral.shared.analytics.constants.Features
import kotlinx.serialization.Serializable

@Serializable
data class EmptyColdStartFeedEvent(
    override val event: String = FeatureEvents.EMPTY_COLD_START_FEED.getEventName(),
    override val featureName: String = Features.FEED.getFeatureName(),
) : EventData
