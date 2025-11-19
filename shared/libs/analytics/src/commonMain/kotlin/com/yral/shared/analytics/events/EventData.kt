package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
sealed interface EventData {
    val event: String
    val featureName: String

    @OptIn(ExperimentalTime::class)
    val timestamp: Long
        get() = Clock.System.now().toEpochMilliseconds()
}

@Serializable
abstract class BaseEventData(
    @SerialName("device") val device: String = "app",
) : EventData

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

fun EventData.shouldAppendUtm(): Boolean =
    when (event) {
        FeatureEvents.SIGNUP_SUCCESS.getEventName() -> true
        FeatureEvents.LOGIN_SUCCESS.getEventName() -> true
        FeatureEvents.AUTH_FAILED.getEventName() -> true
        FeatureEvents.HOME_PAGE_VIEWED.getEventName() -> true
        else -> false
    }
