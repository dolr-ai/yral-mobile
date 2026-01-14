package com.yral.shared.features.aibot.ui.personality

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import kotlinx.coroutines.launch

private const val TOTAL_STEPS = 5
private const val ROLE_LIMIT = 3
private const val TRAIT_LIMIT = 5
private const val CUSTOM_MAX = 30

data class AIBotPersonalityOption(
    val id: String,
    val label: String,
)

data class AIBotPersonalityUiState(
    val selectedRoles: Set<String> = emptySet(),
    val selectedTraits: Set<String> = emptySet(),
    val roleOptions: List<AIBotPersonalityOption> = emptyList(),
    val traitOptions: List<AIBotPersonalityOption> = emptyList(),
) {
    val canProceed: Boolean
        get() = selectedRoles.isNotEmpty() && selectedTraits.isNotEmpty()
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBotPersonalityScreen(
    state: AIBotPersonalityUiState,
    onBackClick: () -> Unit,
    onRoleToggle: (String) -> Unit,
    onTraitToggle: (String) -> Unit,
    onAddCustomRole: (String) -> Unit,
    onAddCustomTrait: (String) -> Unit,
    onNextClick: () -> Unit,
) {
    val topography = LocalAppTopography.current
    val scrollState = rememberScrollState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var addType by remember { mutableStateOf<AddType?>(null) }
    var customText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun openAddSheet(type: AddType) {
        addType = type
        customText = ""
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(YralColors.Neutral950)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar(onBackClick = onBackClick)
        StepProgress(activeIndex = 1, totalSteps = TOTAL_STEPS)

        Text(
            text = "How would you describe your AI's personality?",
            style = topography.lgBold,
            color = YralColors.NeutralTextPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        SectionHeader(
            title = "What's the role of your AI?",
            subtitle = "Choose up to three options or add your own.",
        )
        ChipGroup(
            options = state.roleOptions,
            selectedIds = state.selectedRoles,
            onToggle = onRoleToggle,
            maxSelection = ROLE_LIMIT,
            onAddOwn = { openAddSheet(AddType.Role) },
        )

        SectionHeader(
            title = "What are some of your AI's personality traits?",
            subtitle = "Choose up to five options or add your own.",
        )
        ChipGroup(
            options = state.traitOptions,
            selectedIds = state.selectedTraits,
            onToggle = onTraitToggle,
            maxSelection = TRAIT_LIMIT,
            onAddOwn = { openAddSheet(AddType.Trait) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        YralButton(
            text = "Next",
            backgroundColor = if (state.canProceed) YralColors.Blue300 else YralColors.Neutral800,
            textStyle =
                topography.mdBold.copy(
                    color = if (state.canProceed) YralColors.Neutral50 else YralColors.NeutralTextSecondary,
                    textAlign = TextAlign.Center,
                ),
            buttonState = if (state.canProceed) YralButtonState.Enabled else YralButtonState.Disabled,
            borderColor = if (state.canProceed) YralColors.Blue300 else YralColors.Neutral800,
            onClick = {
                if (state.canProceed) onNextClick()
            },
        )
    }

    AddOwnBottomSheet(
        addType = addType,
        customText = customText,
        sheetState = bottomSheetState,
        onTextChange = { if (it.length <= CUSTOM_MAX) customText = it },
        onSubmit = {
            val trimmed = customText.trim()
            if (trimmed.isNotEmpty()) {
                when (addType) {
                    AddType.Role -> onAddCustomRole(trimmed)
                    AddType.Trait -> onAddCustomTrait(trimmed)
                    null -> Unit
                }
            }
            scope.launch { bottomSheetState.hide() }
            customText = ""
            addType = null
        },
        onDismiss = {
            scope.launch { bottomSheetState.hide() }
            customText = ""
            addType = null
        },
        focusRequester = focusRequester,
        keyboardController = keyboardController,
    )
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
    activeColor: Color = YralColors.Yellow200,
    inactiveColor: Color = YralColors.Neutral800,
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
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    val topography = LocalAppTopography.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = topography.baseBold,
            color = YralColors.NeutralTextPrimary,
        )
        Text(
            text = subtitle,
            style = topography.baseRegular,
            color = YralColors.NeutralTextSecondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    options: List<AIBotPersonalityOption>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    maxSelection: Int,
    onAddOwn: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            val isSelected = selectedIds.contains(option.id)
            Chip(
                text = option.label,
                selected = isSelected,
                enabled = isSelected || selectedIds.size < maxSelection,
                onClick = { onToggle(option.id) },
            )
        }
        AddOwnChip(onClick = onAddOwn)
    }
}

@Composable
private fun Chip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val topography = LocalAppTopography.current
    val background =
        when {
            selected -> YralColors.Blue500
            else -> YralColors.Neutral800
        }
    val borderColor =
        when {
            selected -> YralColors.Blue100
            else -> YralColors.Neutral700
        }
    val textColor =
        when {
            selected -> YralColors.Neutral50
            enabled -> YralColors.NeutralTextPrimary
            else -> YralColors.NeutralTextSecondary
        }

    Box(
        modifier =
            Modifier
                .background(background, RoundedCornerShape(20.dp))
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick, enabled = enabled)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = topography.baseMedium,
            color = textColor,
        )
    }
}

@Composable
private fun AddOwnChip(onClick: () -> Unit) {
    val topography = LocalAppTopography.current
    Box(
        modifier =
            Modifier
                .background(YralColors.Neutral800, RoundedCornerShape(20.dp))
                .border(1.dp, YralColors.Neutral700, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "+ Add your own",
            style = topography.baseMedium,
            color = YralColors.BlueTextPrimary,
        )
    }
}

private enum class AddType { Role, Trait }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
private fun AddOwnBottomSheet(
    addType: AddType?,
    customText: String,
    sheetState: SheetState,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    focusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
) {
    val topography = LocalAppTopography.current

    if (addType != null) {
        LaunchedEffect(addType) {
            sheetState.show()
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        YralBottomSheet(
            onDismissRequest = onDismiss,
            bottomSheetState = sheetState,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text =
                        when (addType) {
                            AddType.Role -> "Role"
                            AddType.Trait -> "Personality trait"
                        },
                    style = topography.lgBold,
                    color = YralColors.NeutralTextPrimary,
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(YralColors.Neutral900, RoundedCornerShape(12.dp))
                            .border(1.dp, YralColors.Neutral800, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "${customText.length}/$CUSTOM_MAX",
                        style = topography.regRegular,
                        color = YralColors.NeutralTextSecondary,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = customText,
                        onValueChange = onTextChange,
                        singleLine = true,
                        textStyle = topography.baseRegular.copy(color = YralColors.NeutralTextPrimary),
                        cursorBrush =
                            androidx.compose.ui.graphics
                                .SolidColor(YralColors.Pink300),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            Box {
                                if (customText.isEmpty()) {
                                    Text(
                                        text = "",
                                        style = topography.baseRegular,
                                        color = YralColors.NeutralTextTertiary,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                val hasText = customText.isNotBlank()
                YralButton(
                    text = "Submit",
                    backgroundColor = if (hasText) YralColors.Blue300 else YralColors.Neutral800,
                    textStyle =
                        topography.mdBold.copy(
                            color = if (hasText) YralColors.Neutral50 else YralColors.NeutralTextSecondary,
                            textAlign = TextAlign.Center,
                        ),
                    buttonState = if (hasText) YralButtonState.Enabled else YralButtonState.Disabled,
                    borderColor = if (hasText) YralColors.Blue300 else YralColors.Neutral800,
                    onClick = {
                        if (hasText) {
                            onSubmit()
                        }
                    },
                )
            }
        }
    }
}
