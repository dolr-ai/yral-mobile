package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
sealed interface EventData {
    val event: String
    val featureName: String
    val timestamp: Long
        get() = Clock.System.now().toEpochMilliseconds()
}

fun EventData.shouldSendToYralBE(): Boolean =
    when (event) {
        FeatureEvents.VIDEO_DURATION_WATCHED.getEventName() -> true
        else -> false
    }

fun EventData.shouldSendToFacebook(): Boolean =
    when (event) {
        FeatureEvents.LOGIN_SUCCESS.getEventName() -> true
        FeatureEvents.GAME_PLAYED.getEventName() -> true
        else -> false
    }
