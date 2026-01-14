package com.yral.shared.features.aibot.ui.name

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

private const val TOTAL_STEPS = 5
private const val NAME_LIMIT = 30

data class AIBotNameUiState(
    val name: String = "",
    val suggestions: List<String> = emptyList(),
)

@Composable
fun AIBotNameScreen(
    state: AIBotNameUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onNextClick: () -> Unit,
) {
    val topography = LocalAppTopography.current
    val nextEnabled = state.name.isNotBlank()

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
        StepProgress(activeIndex = 3, totalSteps = TOTAL_STEPS)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "What's your AI's name?",
                style = topography.lgBold,
                color = YralColors.NeutralTextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            AvatarPlaceholder()
            NameField(
                value = state.name,
                onValueChange = { if (it.length <= NAME_LIMIT) onNameChange(it) },
                limit = NAME_LIMIT,
            )
            SuggestionsRow(
                suggestions = state.suggestions,
                onSuggestionClick = onSuggestionClick,
                isVisible = state.name.isBlank(),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        YralButton(
            text = "Next",
            backgroundColor = if (nextEnabled) YralColors.Blue300 else YralColors.Neutral800,
            textStyle =
                topography.mdBold.copy(
                    color = if (nextEnabled) YralColors.Neutral50 else YralColors.NeutralTextSecondary,
                    textAlign = TextAlign.Center,
                ),
            buttonState = if (nextEnabled) YralButtonState.Enabled else YralButtonState.Disabled,
            borderColor = if (nextEnabled) YralColors.Blue300 else YralColors.Neutral800,
            onClick = {
                if (nextEnabled) onNextClick()
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
private fun AvatarPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .background(YralColors.Neutral800, CircleShape)
                    .border(2.dp, YralColors.Neutral700, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Avatar",
                style = LocalAppTopography.current.baseMedium,
                color = YralColors.NeutralTextSecondary,
            )
        }
    }
}

@Composable
private fun NameField(
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
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Name",
                style = topography.baseRegular,
                color = YralColors.NeutralTextSecondary,
            )
            Text(
                text = "${value.length}/$limit",
                style = topography.regRegular,
                color = YralColors.NeutralTextSecondary,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = topography.baseRegular.copy(color = YralColors.NeutralTextPrimary),
            cursorBrush = SolidColor(YralColors.Pink300),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SuggestionsRow(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    isVisible: Boolean,
) {
    if (suggestions.isEmpty() || !isVisible) return
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
        )
    }
}
