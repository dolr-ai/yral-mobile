package com.yral.shared.libs.designsystem.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class TextFieldValueStateTest {
    @Test
    fun syncTextKeepsCurrentValueWhenTextIsUnchanged() {
        val value =
            TextFieldValue(
                text = "hello world",
                selection = TextRange(0, 5),
            )

        assertSame(value, value.syncText("hello world"))
    }

    @Test
    fun syncTextMovesCursorToEndWhenExternalTextChanges() {
        val value =
            TextFieldValue(
                text = "hello",
                selection = TextRange(0, 5),
            )

        val synced = value.syncText("updated")

        assertEquals("updated", synced.text)
        assertEquals(TextRange(7), synced.selection)
    }

    @Test
    fun limitTextLengthCoercesSelectionToLimitedText() {
        val value =
            TextFieldValue(
                text = "hello world",
                selection = TextRange(3, 10),
            )

        val limited = value.limitTextLength(5)

        assertEquals("hello", limited.text)
        assertEquals(TextRange(3, 5), limited.selection)
    }

    @Test
    fun filterDigitsMapsSelectionToKeptDigits() {
        val value =
            TextFieldValue(
                text = "a1b23c4",
                selection = TextRange(3, 6),
            )

        val filtered = value.filterDigits()

        assertEquals("1234", filtered.text)
        assertEquals(TextRange(1, 3), filtered.selection)
    }

    @Test
    fun filterDigitsLimitsLengthAndCoercesSelection() {
        val value =
            TextFieldValue(
                text = "12a345",
                selection = TextRange(6),
            )

        val filtered = value.filterDigits(maxLength = 3)

        assertEquals("123", filtered.text)
        assertEquals(TextRange(3), filtered.selection)
    }
}
