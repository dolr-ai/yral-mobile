package com.yral.shared.testsupport.analytics

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData

class RecordingAnalyticsProvider(
    override val name: String = "recording",
    private val shouldTrack: (EventData) -> Boolean = { true },
) : AnalyticsProvider {
    val events = mutableListOf<EventData>()
    val users = mutableListOf<User>()
    val commonContexts = mutableListOf<Map<String, Any?>>()
    var resetCount: Int = 0
        private set
    var flushCount: Int = 0
        private set

    override fun shouldTrackEvent(event: EventData): Boolean = shouldTrack(event)

    override fun trackEvent(event: EventData) {
        events += event
    }

    override fun flush() {
        flushCount += 1
    }

    override fun setUserProperties(user: User) {
        users += user
    }

    override fun reset() {
        resetCount += 1
    }

    override fun applyCommonContext(common: Map<String, Any?>) {
        commonContexts += common
    }

    override fun toValidKeyName(key: String): String = key
}
