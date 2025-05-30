package com.yral.shared.analytics.constants

enum class FeatureEvents {
    AUTH_SUCCESSFUL,
    VIDEO_DURATION_WATCHED,
    ;

    fun getEventName(): String =
        when (this) {
            AUTH_SUCCESSFUL -> name.lowercase()
            VIDEO_DURATION_WATCHED -> "VideoDurationWatched"
        }
}
