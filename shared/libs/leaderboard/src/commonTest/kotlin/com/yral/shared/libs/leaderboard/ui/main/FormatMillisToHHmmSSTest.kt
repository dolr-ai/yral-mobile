package com.yral.shared.libs.leaderboard.ui.main

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatMillisToHHmmSSTest {
    @Test
    fun `formats milliseconds into HH colon mm colon ss`() {
        val cases =
            listOf(
                0L to "00:00:00",
                999L to "00:00:00",
                1_000L to "00:00:01",
                61_000L to "00:01:01",
                3_726_500L to "01:02:06",
                25 * 60 * 60 * 1_000L to "25:00:00",
            )

        cases.forEach { (millis, expected) ->
            val formatted = formatMillisToHHmmSS(millis)
            assertEquals(expected, formatted, "Expected $millis ms to format to $expected")
        }
    }
}
