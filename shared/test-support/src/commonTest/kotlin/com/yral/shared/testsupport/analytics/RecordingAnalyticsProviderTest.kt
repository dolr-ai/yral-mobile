package com.yral.shared.testsupport.analytics

import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.BaseEventData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RecordingAnalyticsProviderTest {
    @Test
    fun tracks_events_users_and_context() {
        val event = TestEventData()
        val user = User(userId = "user-1", canisterId = "canister-1")
        val context = mapOf("source" to "test")
        val provider = RecordingAnalyticsProvider()

        assertTrue(provider.shouldTrackEvent(event))

        provider.trackEvent(event)
        provider.setUserProperties(user)
        provider.applyCommonContext(context)
        provider.flush()
        provider.reset()

        assertSame(event, provider.events.single())
        assertSame(user, provider.users.single())
        assertEquals(context, provider.commonContexts.single())
        assertEquals(1, provider.flushCount)
        assertEquals(1, provider.resetCount)
    }

    @Test
    fun can_filter_events() {
        val provider = RecordingAnalyticsProvider(shouldTrack = { false })

        assertFalse(provider.shouldTrackEvent(TestEventData()))
    }
}

private data class TestEventData(
    override val event: String = "test_event",
    override val featureName: String = "test_feature",
) : BaseEventData()
