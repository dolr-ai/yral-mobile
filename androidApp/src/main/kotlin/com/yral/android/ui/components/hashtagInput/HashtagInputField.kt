package com.yral.android.ui.components.hashtagInput

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

@Suppress("LongMethod")
@Composable
fun ChipInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit = {},
    placeholder: String,
    showHash: Boolean = true,
    focusRequester: FocusRequester? = null,
    setFocus: (Boolean) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Keep textFieldValue in sync with value from parent
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue =
                TextFieldValue(
                    text = value,
                    selection = TextRange(value.length),
                )
        }
    }

    Box(
        modifier =
            Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(16.dp)),
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onValueChange(it.text)
            },
            singleLine = true,
            textStyle = LocalAppTopography.current.regRegular.copy(color = YralColors.Neutral300),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.None,
                ),
            keyboardActions =
                KeyboardActions(
                    onAny = {
                        onDone()
                        keyboardController?.hide()
                    },
                ),
            modifier =
                Modifier
                    .wrapContentSize()
                    .onKeyEvent {
                        when (it.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_ENTER -> {
                                onDone()
                                true
                            }
                            KeyEvent.KEYCODE_DEL -> {
                                if (value.isEmpty()) {
                                    onValueChange(value)
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }.onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        setFocus(isFocused)
                        if (focusState.isFocused && textFieldValue.text.isNotEmpty()) {
                            textFieldValue =
                                textFieldValue.copy(
                                    selection = TextRange(textFieldValue.text.length),
                                )
                        }
                    }.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    if (showHash) {
                        Text(
                            text = "#",
                            style = LocalAppTopography.current.regRegular,
                            color = Color.White,
                        )
                    }
                    if (textFieldValue.text.isEmpty() && !isFocused) {
                        Text(
                            text = placeholder,
                            style = LocalAppTopography.current.regRegular,
                            color = YralColors.Neutral600,
                        )
                    } else {
                        innerTextField()
                    }
                }
            },
            cursorBrush = SolidColor(TextFieldDefaults.colors().cursorColor),
        )
    }
}

@Composable
fun HashtagInputField(
    value: String,
    placeholder: String,
    shouldFocusInputField: Boolean,
    updateShouldFocusInputField: (Boolean) -> Unit,
    setFocus: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(shouldFocusInputField) {
        if (shouldFocusInputField) {
            focusRequester.requestFocus()
            updateShouldFocusInputField(false)
        }
    }
    ChipInputField(
        value = value,
        onValueChange = onValueChange,
        onDone = onDone,
        placeholder = placeholder,
        showHash = false,
        focusRequester = focusRequester,
        setFocus = setFocus,
    )
}

@Composable
fun keyboardHeightAsState(): State<Int> {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val heightPx = ime.getBottom(density)
    return rememberUpdatedState(heightPx)
}
