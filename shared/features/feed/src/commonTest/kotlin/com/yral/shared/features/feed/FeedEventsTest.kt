package com.yral.shared.features.feed

import com.yral.shared.analytics.events.FeedLoaderShownEventData
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedEventsTest {
    @Test
    fun feedLoaderShownEventDataHasCorrectEventName() {
        val eventData = FeedLoaderShownEventData(durationMs = 1500L)
        assertEquals("feed_loader_shown", eventData.event)
    }

    @Test
    fun feedLoaderShownEventDataHasCorrectFeatureName() {
        val eventData = FeedLoaderShownEventData(durationMs = 1500L)
        assertEquals("feed", eventData.featureName)
    }

    @Test
    fun feedLoaderShownEventDataStoresDurationCorrectly() {
        val eventData = FeedLoaderShownEventData(durationMs = 3200L)
        assertEquals(3200L, eventData.durationMs)
    }

    @Test
    fun feedLoaderShownEventDataHandlesZeroDuration() {
        val eventData = FeedLoaderShownEventData(durationMs = 0L)
        assertEquals(0L, eventData.durationMs)
    }

    @Test
    fun feedLoaderShownEventDataHandlesLargeDuration() {
        val eventData = FeedLoaderShownEventData(durationMs = 60_000L)
        assertEquals(60_000L, eventData.durationMs)
    }

    @Test
    fun feedLoaderShownSecondaryConstructorSetsAllFields() {
        val eventData = FeedLoaderShownEventData(durationMs = 500L)
        assertEquals("feed_loader_shown", eventData.event)
        assertEquals("feed", eventData.featureName)
        assertEquals(500L, eventData.durationMs)
    }
}
