package com.yral.android.installReferrer

import com.yral.shared.preferences.stores.UtmParams
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtmParamsExtensionsTest {
    @Test
    fun `test isEmpty returns true when all fields are null`() {
        val params = UtmParams()
        assertTrue(params.isEmpty(), "Should be empty when all fields are null")
    }

    @Test
    fun `test isEmpty returns true when all fields are blank`() {
        val params =
            UtmParams(
                source = "",
                medium = "   ",
                campaign = "",
                term = "",
                content = "",
            )
        assertTrue(params.isEmpty(), "Should be empty when all fields are blank")
    }

    @Test
    fun `test isEmpty returns false when any field has value`() {
        val params = UtmParams(source = "test")
        assertFalse(params.isEmpty(), "Should not be empty when source has value")
    }

    @Test
    fun `test isEmpty returns false when campaign has value`() {
        val params = UtmParams(campaign = "test_campaign")
        assertFalse(params.isEmpty(), "Should not be empty when campaign has value")
    }

    @Test
    fun `test isEmpty returns false when medium has value`() {
        val params = UtmParams(medium = "test_medium")
        assertFalse(params.isEmpty(), "medium should not be empty when medium has value")
    }

    @Test
    fun `test isEmpty returns false when term has value`() {
        val params = UtmParams(term = "test_term")
        assertFalse(params.isEmpty(), "term should not be empty when term has value")
    }

    @Test
    fun `test isEmpty returns false when content has value`() {
        val params = UtmParams(content = "test_content")
        assertFalse(params.isEmpty(), "content should not be empty when content has value")
    }

    @Test
    fun `test isNotEmpty returns true when params have values`() {
        val params =
            UtmParams(
                source = "test_source",
                campaign = "test_campaign",
            )
        assertTrue(params.isNotEmpty(), "Should not be empty when params have values")
    }

    @Test
    fun `test isNotEmpty returns false when params are empty`() {
        val params = UtmParams()
        assertFalse(params.isNotEmpty(), "Should be empty when all fields are null")
    }
}
