package com.yral.android.ui.screens.uploadVideo.aiVideoGen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors

@Suppress("LongMethod")
@Composable
fun PromptInput(
    text: String,
    onValueChange: (text: String) -> Unit,
    onHeightChange: (height: Int) -> Unit,
) {
    Column(
        modifier = Modifier.onGloballyPositioned { onHeightChange(it.size.height) },
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.prompt),
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral300,
        )
        var isFocused by remember { mutableStateOf(false) }
        val maxChars = AiVideoGenScreenConstants.PROMPT_MAX_CHAR
        val showCounter = isFocused || text.isNotEmpty()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(YralColors.Neutral800, RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = if (isFocused) YralColors.Neutral500 else Color.Transparent,
                        shape = RoundedCornerShape(size = 8.dp),
                    ).clip(RoundedCornerShape(8.dp)),
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        ) {
            TextField(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                value = text,
                onValueChange = { newValue -> onValueChange(newValue.take(maxChars)) },
                colors =
                    TextFieldDefaults.colors().copy(
                        focusedTextColor = YralColors.Neutral300,
                        unfocusedTextColor = YralColors.Neutral300,
                        disabledTextColor = YralColors.Neutral600,
                        focusedContainerColor = YralColors.Neutral800,
                        unfocusedContainerColor = YralColors.Neutral800,
                        disabledContainerColor = YralColors.Neutral800,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                textStyle = LocalAppTopography.current.baseRegular,
                placeholder = {
                    Text(
                        text = stringResource(R.string.enter_prompt_here),
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.Neutral600,
                    )
                },
                minLines = 5,
                maxLines = 5,
            )
            if (showCounter) {
                Text(
                    text = "${text.length}/$maxChars",
                    style = LocalAppTopography.current.regRegular,
                    color = YralColors.Neutral600,
                    textAlign = TextAlign.End,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .offset(y = (-12).dp)
                            .padding(start = 12.dp, end = 12.dp),
                )
            }
        }
    }
}
