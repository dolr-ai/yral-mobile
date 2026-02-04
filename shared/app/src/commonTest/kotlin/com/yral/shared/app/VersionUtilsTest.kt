package com.yral.shared.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionUtilsTest {
    @Test
    fun `isVersionLower returns true when current major is less than min`() {
        assertTrue(isVersionLower("1.0.0", "2.0.0"))
        assertTrue(isVersionLower("0.9.9", "1.0.0"))
    }

    @Test
    fun `isVersionLower returns false when current major is greater than min`() {
        assertFalse(isVersionLower("2.0.0", "1.0.0"))
        assertFalse(isVersionLower("1.0.0", "0.9.9"))
    }

    @Test
    fun `isVersionLower returns true when current minor is less than min`() {
        assertTrue(isVersionLower("1.0.0", "1.1.0"))
        assertTrue(isVersionLower("1.1.0", "1.2.0"))
    }

    @Test
    fun `isVersionLower returns false when current minor is greater than min`() {
        assertFalse(isVersionLower("1.1.0", "1.0.0"))
        assertFalse(isVersionLower("1.2.0", "1.1.0"))
    }

    @Test
    fun `isVersionLower returns true when current patch is less than min`() {
        assertTrue(isVersionLower("1.0.0", "1.0.1"))
        assertTrue(isVersionLower("1.0.1", "1.0.2"))
    }

    @Test
    fun `isVersionLower returns false when current patch is greater than min`() {
        assertFalse(isVersionLower("1.0.1", "1.0.0"))
        assertFalse(isVersionLower("1.0.2", "1.0.1"))
    }

    @Test
    fun `isVersionLower returns false when versions are equal`() {
        assertFalse(isVersionLower("1.0.0", "1.0.0"))
        assertFalse(isVersionLower("2.3.4", "2.3.4"))
    }

    @Test
    fun `isVersionLower returns false when current equals min with different segment length`() {
        assertFalse(isVersionLower("1.0", "1.0.0"))
        assertFalse(isVersionLower("1.0.0", "1.0"))
    }

    @Test
    fun `isVersionLower treats missing segments as zero`() {
        assertTrue(isVersionLower("1.0", "1.0.1"))
        assertFalse(isVersionLower("1.0.1", "1.0"))
    }

    @Test
    fun `isVersionLower returns true when current is single segment lower`() {
        assertTrue(isVersionLower("1", "2"))
        assertTrue(isVersionLower("1", "1.0.1"))
    }

    @Test
    fun `isVersionLower returns false when current is single segment higher or equal`() {
        assertFalse(isVersionLower("2", "1"))
        assertFalse(isVersionLower("1", "1"))
    }

    @Test
    fun `isVersionLower handles empty current as lower`() {
        assertTrue(isVersionLower("", "1.0.0"))
        assertTrue(isVersionLower("", "0.0.1"))
    }

    @Test
    fun `isVersionLower handles empty min - current not lower`() {
        assertFalse(isVersionLower("1.0.0", ""))
        assertFalse(isVersionLower("0.0.1", ""))
    }

    @Test
    fun `isVersionLower compares segments numerically not lexicographically`() {
        assertTrue(isVersionLower("1.9.0", "1.10.0"))
        assertFalse(isVersionLower("1.10.0", "1.9.0"))
    }

    @Test
    fun `isVersionLower treats invalid segment as zero`() {
        assertTrue(isVersionLower("1.a.0", "1.1.0"))
        assertFalse(isVersionLower("1.1.0", "1.a.0"))
    }
}
