package com.yral.shared.features.aibot.ui.description

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

private const val DESCRIPTION_LIMIT = 2000
private const val TOTAL_STEPS = 5

data class AIBotDescriptionUiState(
    val description: String = "",
    val suggestions: List<String> = emptyList(),
)

@Composable
fun AIBotDescriptionScreen(
    state: AIBotDescriptionUiState,
    onBackClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onNextClick: () -> Unit,
) {
    val topography = LocalAppTopography.current
    val nextEnabled = state.description.isNotBlank()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(YralColors.Neutral950)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar(onBackClick = onBackClick)
        StepProgress(activeIndex = 0, totalSteps = TOTAL_STEPS)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "What does your AI do and what makes it unique?",
                style = topography.lgBold,
                color = YralColors.NeutralTextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            DescriptionField(
                value = state.description,
                onValueChange = { newValue ->
                    if (newValue.length <= DESCRIPTION_LIMIT) {
                        onDescriptionChange(newValue)
                    }
                },
                limit = DESCRIPTION_LIMIT,
            )
            SuggestionsSection(
                suggestions = state.suggestions,
                onSuggestionClick = onSuggestionClick,
                isVisible = state.description.isBlank(),
            )
        }
        Box(modifier = Modifier.weight(1f))
        YralButton(
            text = "Next",
            backgroundColor = if (nextEnabled) YralColors.Blue300 else YralColors.Neutral800,
            textStyle =
                topography.mdBold.copy(
                    color = if (nextEnabled) YralColors.Neutral50 else YralColors.NeutralTextSecondary,
                ),
            buttonState = if (nextEnabled) YralButtonState.Enabled else YralButtonState.Disabled,
            borderColor = if (nextEnabled) YralColors.Blue300 else YralColors.Neutral800,
            onClick = {
                if (nextEnabled) {
                    onNextClick()
                }
            },
        )
    }
}

@Composable
private fun TopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = YralColors.NeutralTextPrimary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Create an AI",
            style = LocalAppTopography.current.mdSemiBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Composable
private fun StepProgress(
    activeIndex: Int,
    totalSteps: Int,
    activeColor: androidx.compose.ui.graphics.Color = YralColors.Yellow200,
    inactiveColor: androidx.compose.ui.graphics.Color = YralColors.Neutral800,
    height: Dp = 4.dp,
    spacing: Dp = 6.dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(height)
                        .background(
                            color = if (index <= activeIndex) activeColor else inactiveColor,
                            shape = RoundedCornerShape(999.dp),
                        ),
            )
        }
    }
}

@Composable
private fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    limit: Int,
) {
    val topography = LocalAppTopography.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Neutral900, RoundedCornerShape(12.dp))
                .border(1.dp, YralColors.Neutral800, RoundedCornerShape(12.dp))
                .height(220.dp)
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Describe your AI",
                style = topography.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
            Text(
                text = "${value.length}/$limit",
                style = topography.regRegular,
                color = YralColors.NeutralTextSecondary,
                textAlign = TextAlign.End,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = topography.baseRegular.copy(color = YralColors.NeutralTextPrimary),
            cursorBrush = SolidColor(YralColors.Pink300),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .weight(1f),
        )
    }
}

@Composable
private fun SuggestionsSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    isVisible: Boolean,
) {
    if (!isVisible || suggestions.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(suggestions) { suggestion ->
            SuggestionChip(
                text = suggestion,
                onClick = { onSuggestionClick(suggestion) },
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .background(YralColors.Neutral800, RoundedCornerShape(16.dp))
                .border(1.dp, YralColors.Neutral700, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 260.dp),
    ) {
        Text(
            text = text,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.BlueTextPrimary,
            maxLines = 2,
        )
    }
}
