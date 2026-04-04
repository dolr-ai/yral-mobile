package com.yral.shared.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatUnreadBadgeTest {
    @Test
    fun `chat unread badge text hides badge when count is zero or less`() {
        assertEquals(null, chatUnreadBadgeText(0))
        assertEquals(null, chatUnreadBadgeText(-3))
    }

    @Test
    fun `chat unread badge text shows raw count up to ninety nine`() {
        assertEquals("7", chatUnreadBadgeText(7))
        assertEquals("99", chatUnreadBadgeText(99))
    }

    @Test
    fun `chat unread badge text caps at ninety nine plus`() {
        assertEquals("99+", chatUnreadBadgeText(100))
        assertEquals("99+", chatUnreadBadgeText(Int.MAX_VALUE))
    }
}
