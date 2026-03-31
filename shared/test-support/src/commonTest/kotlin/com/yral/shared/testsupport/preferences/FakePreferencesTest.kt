package com.yral.shared.testsupport.preferences

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakePreferencesTest {
    @Test
    fun storesAndRemovesValues() =
        runTest {
            val preferences = FakePreferences()

            preferences.putString("key", "value")
            assertEquals("value", preferences.getString("key"))

            preferences.remove("key")
            assertNull(preferences.getString("key"))
        }
}
