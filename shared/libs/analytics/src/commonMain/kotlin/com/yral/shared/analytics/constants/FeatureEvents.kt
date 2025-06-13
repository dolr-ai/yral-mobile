package com.yral.shared.analytics.constants

enum class FeatureEvents {
    AUTH_SUCCESSFUL,
    VIDEO_DURATION_WATCHED,
    EMPTY_COLD_START_FEED,
    ;

    fun getEventName(): String =
        when (this) {
            AUTH_SUCCESSFUL -> name.lowercase()
            EMPTY_COLD_START_FEED -> name.lowercase()
            VIDEO_DURATION_WATCHED -> "VideoDurationWatched"
        }
}
