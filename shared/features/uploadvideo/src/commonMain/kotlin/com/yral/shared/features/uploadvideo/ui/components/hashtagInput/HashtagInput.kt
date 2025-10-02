package com.yral.shared.features.uploadvideo.ui.components.hashtagInput

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.add_hashtags
import yral_mobile.shared.features.uploadvideo.generated.resources.hit_enter_to_add_hashtags

@Suppress("LongMethod")
@Composable
fun HashtagInput(
    hashtags: List<String>,
    onHashtagsChange: (List<String>) -> Unit,
) {
    val state = rememberHashtagInputState(hashtags, onHashtagsChange)
    val scrollState = rememberScrollState()
    var isChipFocused by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isInputFocused, state) {
        if (isInputFocused) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = if (isChipFocused || isInputFocused) YralColors.Neutral500 else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                ).background(YralColors.Neutral900)
                .padding(vertical = 12.dp)
                .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hashtags.isNotEmpty()) {
            Row(modifier = Modifier.padding(start = 6.dp)) {
                hashtags.forEachIndexed { index, tag ->
                    if (state.editingIndex == index) {
                        HashtagEditChip(
                            value = state.inputText,
                            onValueChange = { newValue ->
                                // Save previous text BEFORE handling the change
                                state.handleEditChipValueChange(newValue, index)
                            },
                            onDone = {
                                state.handleEditChipDone(state.editingIndex ?: index)
                                isChipFocused = false
                            },
                            setFocus = { isChipFocused = true },
                        )
                    } else {
                        HashtagChip(
                            tag = tag,
                            onClick = {
                                state.updateEditingIndex(index)
                                state.updateInputText(tag)
                            },
                        )
                    }
                }
            }
        }
        if (state.editingIndex == null) {
            HashtagInputField(
                value = state.inputText,
                placeholder =
                    if (hashtags.isEmpty()) {
                        stringResource(Res.string.hit_enter_to_add_hashtags)
                    } else {
                        stringResource(Res.string.add_hashtags)
                    },
                shouldFocusInputField = state.shouldFocusInputField,
                updateShouldFocusInputField = { shouldFocus ->
                    state.updateShouldFocusInputField(shouldFocus)
                },
                setFocus = { isInputFocused = it },
                onValueChange = { newValue ->
                    state.handleInputFieldValueChange(newValue)
                },
                onDone = {
                    state.handleInputFieldDone()
                },
            )
        }
    }
}
