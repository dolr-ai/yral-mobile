package com.yral.shared.features.auth.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

@Suppress("LongMethod")
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    config: OtpInputConfig = OtpInputConfig(),
    enabled: Boolean = true,
    onOtpComplete: ((String) -> Unit)? = null,
) {
    val length = config.length
    val sanitizedValue = remember(value, length) { value.filter(Char::isDigit).take(length) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = sanitizedValue,
                selection = TextRange(sanitizedValue.length),
            ),
        )
    }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val activeIndex =
        if (!isFocused) -1 else textFieldValue.selection.start.coerceIn(0, length - 1)

    LaunchedEffect(sanitizedValue) {
        if (textFieldValue.text != sanitizedValue) {
            textFieldValue =
                TextFieldValue(
                    text = sanitizedValue,
                    selection = TextRange(sanitizedValue.length),
                )
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val filtered = newValue.text.filter(Char::isDigit).take(length)
            val selection = newValue.selection.start.coerceIn(0, filtered.length)
            val updatedFieldValue = TextFieldValue(filtered, TextRange(selection))
            textFieldValue = updatedFieldValue
            onValueChange(filtered)
            if (filtered.length == length) {
                onOtpComplete?.invoke(filtered)
            }
        },
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .defaultMinSize(minHeight = config.boxHeight),
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        textStyle =
            LocalAppTopography.current.lgBold.copy(
                color = Color.Transparent,
                textAlign = TextAlign.Center,
            ),
        decorationBox = { innerTextField ->
            OtpBoxes(
                value = textFieldValue.text,
                activeIndex = activeIndex,
                config = config,
                enabled = enabled,
                onBoxClick = { index ->
                    val truncated = textFieldValue.text.take(index)
                    val selection = truncated.length
                    textFieldValue = TextFieldValue(truncated, TextRange(selection))
                    onValueChange(truncated)
                    focusRequester.requestFocus()
                },
            )
            Box(modifier = Modifier.alpha(0f)) {
                innerTextField()
            }
        },
        interactionSource = interactionSource,
    )
}

@Composable
private fun OtpBoxes(
    value: String,
    activeIndex: Int,
    config: OtpInputConfig,
    enabled: Boolean,
    onBoxClick: (Int) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = config.boxWidth * config.length + (config.length * 20).dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(config.length) { index ->
                val digit = value.getOrNull(index)?.toString().orEmpty()
                OtpBox(
                    value = digit,
                    isActive = activeIndex == index,
                    config = config,
                    enabled = enabled,
                    onClick = { onBoxClick(index) },
                )
            }
        }
    }
}

@Composable
private fun OtpBox(
    value: String,
    isActive: Boolean,
    config: OtpInputConfig,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        if (isActive) YralColors.Pink300 else YralColors.Neutral700,
        label = "otpBoxBorder",
    )
    val backgroundColor = if (enabled) YralColors.Neutral900 else YralColors.Neutral800
    val textStyle: TextStyle =
        LocalAppTopography.current.lgBold.copy(
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )

    Box(
        modifier =
            Modifier
                .width(config.boxWidth)
                .height(config.boxHeight)
                .background(backgroundColor, RoundedCornerShape(config.cornerRadius))
                .border(
                    width = config.borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(config.cornerRadius),
                ).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            style = textStyle,
            maxLines = 1,
        )
    }
}

data class OtpInputConfig(
    val length: Int = 6,
    val boxWidth: Dp = 43.dp,
    val boxHeight: Dp = 53.dp,
    val spacing: Dp = 20.dp,
    val cornerRadius: Dp = 8.dp,
    val borderWidth: Dp = 1.dp,
)
