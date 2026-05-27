package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun rememberTextFieldValueState(text: String): MutableState<TextFieldValue> {
    val state = remember { mutableStateOf(text.toTextFieldValueAtEnd()) }
    LaunchedEffect(text) {
        state.value = state.value.syncText(text)
    }
    return state
}

fun TextFieldValue.syncText(text: String): TextFieldValue =
    if (text == this.text) {
        this
    } else {
        text.toTextFieldValueAtEnd()
    }

fun TextFieldValue.limitTextLength(maxLength: Int): TextFieldValue {
    require(maxLength >= 0) { "maxLength must be non-negative" }
    if (text.length <= maxLength) return this
    val limitedText = text.take(maxLength)
    return copy(
        text = limitedText,
        selection = selection.coerceIn(limitedText.length),
        composition = composition?.coerceIn(limitedText.length),
    )
}

fun TextFieldValue.filterDigits(maxLength: Int = Int.MAX_VALUE): TextFieldValue {
    require(maxLength >= 0) { "maxLength must be non-negative" }
    val limitedText = text.filter(Char::isDigit).take(maxLength)
    return copy(
        text = limitedText,
        selection =
            TextRange(
                start = countDigitsBefore(selection.start).coerceAtMost(limitedText.length),
                end = countDigitsBefore(selection.end).coerceAtMost(limitedText.length),
            ),
        composition = null,
    )
}

private fun String.toTextFieldValueAtEnd(): TextFieldValue =
    TextFieldValue(
        text = this,
        selection = TextRange(length),
    )

private fun TextRange.coerceIn(textLength: Int): TextRange =
    TextRange(
        start = start.coerceIn(0, textLength),
        end = end.coerceIn(0, textLength),
    )

private fun TextFieldValue.countDigitsBefore(offset: Int): Int {
    val coercedOffset = offset.coerceIn(0, text.length)
    return text.take(coercedOffset).count(Char::isDigit)
}
