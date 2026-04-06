package com.yral.android.installReferrer

import android.app.Application
import co.touchlab.kermit.Logger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], application = Application::class)
class InstallReferrerAttributionRobolectricTest {
    @Before
    fun setup() {
        AttributionManager.setLoggerFactory { tag -> Logger.withTag(tag) }
    }

    @After
    fun teardown() {
        AttributionManager.setLoggerFactory(null)
    }

    @Test
    fun `extractUtmParams parses gclid from query referrer`() {
        val attribution = InstallReferrerAttribution(Application())
        val gclid = "Cj0KCQjw7cLOBhDmARIsAGsuA0k-test"
        val raw =
            "utm_source=google&utm_medium=cpc&gclid=$gclid&utm_campaign=spring"

        val params = attribution.extractUtmParams(raw)

        assertEquals(gclid, params.gclid)
        assertNotNull(params.raw)
    }

    @Test
    fun `extractUtmParams infers google_ads when only gclid present`() {
        val attribution = InstallReferrerAttribution(Application())
        val gclid = "EAIw_testgclid"
        val raw = "gclid=$gclid"

        val params = attribution.extractUtmParams(raw)

        assertEquals("google_ads", params.source)
        assertEquals(gclid, params.gclid)
    }
}
