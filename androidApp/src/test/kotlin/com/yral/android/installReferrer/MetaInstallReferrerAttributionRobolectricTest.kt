package com.yral.android.installReferrer

import android.app.Application
import co.touchlab.kermit.Logger
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that require Robolectric for Android API access (e.g., Uri parsing).
 * These tests are separated from regular unit tests to ensure proper Robolectric initialization.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], application = Application::class) // Use minimal Application to avoid YralApp initialization
class MetaInstallReferrerAttributionRobolectricTest {
    @Before
    fun setup() {
        // Set mock logger factory for tests
        AttributionManager.setLoggerFactory { tag -> Logger.withTag(tag) }
    }

    @After
    fun teardown() {
        // Reset logger factory after tests
        AttributionManager.setLoggerFactory(null)
    }

    private fun createTestInstance(): MetaInstallReferrerAttribution = MetaInstallReferrerAttribution()

    @Test
    fun `test convertReferrerToJson handles query string format`() {
        val attribution = createTestInstance()
        // utm_content= is mandatory for Meta Install Referrer
        val queryString = "utm_source=test&utm_campaign=campaign&utm_content=dummy_content"

        val json =
            requireNotNull(
                attribution.convertReferrerToJson(queryString),
            ) { "Should parse query string" }
        assertEquals("test", json["utm_source"]?.jsonPrimitive?.content)
        assertEquals("campaign", json["utm_campaign"]?.jsonPrimitive?.content)
        assertEquals("dummy_content", json["utm_content"]?.jsonPrimitive?.content)
    }
}
