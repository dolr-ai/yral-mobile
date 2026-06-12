package com.yral.shared.features.coach.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.features.coach.domain.models.CoachMessage
import com.yral.shared.features.coach.domain.models.CoachMessageRole
import com.yral.shared.features.coach.nav.CoachComponent
import com.yral.shared.features.coach.nav.OpenCoachParams
import com.yral.shared.features.coach.viewmodel.CoachError
import com.yral.shared.features.coach.viewmodel.CoachViewModel
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showError
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.coach.generated.resources.Res
import yral_mobile.shared.features.coach.generated.resources.coach_apply_confirm_body
import yral_mobile.shared.features.coach.generated.resources.coach_apply_confirm_cancel
import yral_mobile.shared.features.coach.generated.resources.coach_apply_confirm_cta
import yral_mobile.shared.features.coach.generated.resources.coach_apply_confirm_title
import yral_mobile.shared.features.coach.generated.resources.coach_apply_failed
import yral_mobile.shared.features.coach.generated.resources.coach_back
import yral_mobile.shared.features.coach.generated.resources.coach_empty_state_subtitle
import yral_mobile.shared.features.coach.generated.resources.coach_empty_state_title
import yral_mobile.shared.features.coach.generated.resources.coach_header_hint
import yral_mobile.shared.features.coach.generated.resources.coach_input_placeholder
import yral_mobile.shared.features.coach.generated.resources.coach_loading_session
import yral_mobile.shared.features.coach.generated.resources.coach_proposal_applied
import yral_mobile.shared.features.coach.generated.resources.coach_proposal_apply_cta
import yral_mobile.shared.features.coach.generated.resources.coach_proposal_card_title
import yral_mobile.shared.features.coach.generated.resources.coach_save_cta
import yral_mobile.shared.features.coach.generated.resources.coach_save_cta_generic
import yral_mobile.shared.features.coach.generated.resources.coach_screen_title
import yral_mobile.shared.features.coach.generated.resources.coach_send
import yral_mobile.shared.features.coach.generated.resources.coach_send_failed
import yral_mobile.shared.features.coach.generated.resources.coach_session_start_failed
import yral_mobile.shared.features.coach.generated.resources.coach_start_over
import yral_mobile.shared.features.coach.generated.resources.coach_start_over_confirm_body
import yral_mobile.shared.features.coach.generated.resources.coach_start_over_confirm_cancel
import yral_mobile.shared.features.coach.generated.resources.coach_start_over_confirm_cta
import yral_mobile.shared.features.coach.generated.resources.coach_start_over_confirm_title
import yral_mobile.shared.features.coach.generated.resources.coach_thinking
import yral_mobile.shared.features.coach.generated.resources.coach_view_full_prompt
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.ic_send
import yral_mobile.shared.libs.designsystem.generated.resources.ic_send_disabled
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun CoachScreen(
    component: CoachComponent,
    viewModel: CoachViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()
    val params = component.openCoachParams

    CoachScreenEffects(params, viewState.lastAppliedToastMessage, viewModel)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(YralColors.PrimaryContainer)
                .safeDrawingPadding(),
    ) {
        CoachHeader(
            avatarUrl = viewState.avatarUrl,
            botName = viewState.botName,
            showStartOver = viewState.coachConversationId != null,
            onBack = { component.onBack() },
            onStartOver = { viewModel.requestStartOverConfirm() },
            onViewFullPrompt = { component.openSoulFile() },
        )

        CoachHeaderHint()

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (viewState.coachConversationId == null && viewState.isSessionLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.coach_loading_session),
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.NeutralTextSecondary,
                    )
                }
            } else if (viewState.pending.isEmpty()) {
                CoachEmptyState(botName = viewState.botName)
            } else {
                CoachMessagesList(
                    messages = viewState.pending,
                    isCoachThinking = viewState.isCoachThinking,
                    activeProposalMessageId = viewState.activeProposalMessage?.id,
                    openingSuggestions = viewState.openingSuggestions,
                    onApplyClick = { viewModel.requestApplyProposal() },
                    onSuggestionTap = { suggestion -> viewModel.sendMessage(suggestion) },
                )
            }
        }

        viewState.error?.let { err ->
            CoachErrorBanner(error = err, onDismiss = { viewModel.clearError() })
        }

        if (viewState.canRequestSave) {
            CoachSaveButton(
                botName = viewState.botName,
                enabled = !viewState.isCoachThinking,
                onSave = { viewModel.requestSaveProposal() },
            )
        }

        CoachInputArea(
            onSend = { text -> viewModel.sendMessage(text) },
            enabled = viewState.coachConversationId != null && !viewState.isCoachThinking,
        )
    }

    if (viewState.showApplyConfirm) {
        CoachApplyConfirmDialog(
            botName = viewState.botName,
            isApplying = viewState.isApplying,
            onConfirm = { viewModel.confirmApplyProposal() },
            onDismiss = { viewModel.dismissApplyConfirm() },
        )
    }

    if (viewState.showStartOverConfirm) {
        CoachStartOverConfirmDialog(
            onConfirm = {
                viewModel.dismissStartOverConfirm()
                viewModel.startOver()
            },
            onDismiss = { viewModel.dismissStartOverConfirm() },
        )
    }
}

@Composable
private fun CoachScreenEffects(
    params: OpenCoachParams,
    appliedToastMessage: String?,
    viewModel: CoachViewModel,
) {
    LaunchedEffect(params.botId) {
        viewModel.openForBot(
            botId = params.botId,
            botName = params.botName,
            avatarUrl = params.avatarUrl,
            sectionHint = params.sectionHint,
        )
    }

    LaunchedEffect(appliedToastMessage) {
        appliedToastMessage?.let { message ->
            ToastManager.showSuccess(type = ToastType.Small(message = message))
            viewModel.clearAppliedToast()
        }
    }
}

@Composable
private fun CoachHeader(
    avatarUrl: String?,
    botName: String?,
    showStartOver: Boolean,
    onBack: () -> Unit,
    onStartOver: () -> Unit,
    onViewFullPrompt: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.PrimaryContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = stringResource(Res.string.coach_back),
                modifier =
                    Modifier
                        .size(28.dp)
                        .clickable(onClick = onBack),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(36.dp).background(YralColors.Neutral800, CircleShape)) {
                YralAsyncImage(
                    imageUrl = avatarUrl.orEmpty(),
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.coach_screen_title),
                    style = LocalAppTopography.current.regRegular,
                    color = YralColors.NeutralTextSecondary,
                )
                Text(
                    text = botName.orEmpty(),
                    style = LocalAppTopography.current.baseSemiBold,
                    color = YralColors.NeutralTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.coach_view_full_prompt),
                style = LocalAppTopography.current.smSemiBold,
                color = YralColors.Pink300,
                modifier = Modifier.clickable(onClick = onViewFullPrompt).padding(8.dp),
            )
            if (showStartOver) {
                Text(
                    text = stringResource(Res.string.coach_start_over),
                    style = LocalAppTopography.current.smSemiBold,
                    color = YralColors.Pink300,
                    modifier = Modifier.clickable(onClick = onStartOver).padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun CoachHeaderHint() {
    Text(
        text = stringResource(Res.string.coach_header_hint),
        style = LocalAppTopography.current.smRegular,
        color = YralColors.NeutralTextSecondary,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Neutral900)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CoachEmptyState(botName: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.coach_empty_state_title, botName.orEmpty()),
            style = LocalAppTopography.current.xlSemiBold,
            color = YralColors.NeutralTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.coach_empty_state_subtitle),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
        )
    }
}

@Composable
private fun CoachMessagesList(
    messages: List<CoachMessage>,
    isCoachThinking: Boolean,
    activeProposalMessageId: String?,
    openingSuggestions: List<String>,
    onApplyClick: () -> Unit,
    onSuggestionTap: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val rowCount = messages.size + if (openingSuggestions.isNotEmpty()) 1 else 0
    LaunchedEffect(rowCount) {
        if (rowCount > 0) listState.animateScrollToItem(rowCount - 1)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            items = messages,
            key = { it.id },
        ) { msg ->
            CoachMessageBubble(
                message = msg,
                showProposalCard = msg.id == activeProposalMessageId,
                onApplyClick = onApplyClick,
                isCoachThinkingPlaceholder =
                    isCoachThinking &&
                        msg.role == CoachMessageRole.COACH &&
                        msg.content.isEmpty(),
            )
        }
        if (openingSuggestions.isNotEmpty()) {
            item(key = "opening-suggestions") {
                CoachSuggestionChipsRow(
                    suggestions = openingSuggestions,
                    onTap = onSuggestionTap,
                )
            }
        }
    }
}

@Composable
private fun CoachSuggestionChipsRow(
    suggestions: List<String>,
    onTap: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = suggestions, key = { it }) { chipText ->
            Box(
                modifier =
                    Modifier
                        .border(
                            width = 1.dp,
                            color = YralColors.Pink300,
                            shape = RoundedCornerShape(20.dp),
                        ).clickable { onTap(chipText) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = chipText,
                    style = LocalAppTopography.current.smSemiBold,
                    color = YralColors.Pink300,
                )
            }
        }
    }
}

@Composable
private fun CoachMessageBubble(
    message: CoachMessage,
    showProposalCard: Boolean,
    onApplyClick: () -> Unit,
    isCoachThinkingPlaceholder: Boolean,
) {
    val isCreator = message.role == CoachMessageRole.CREATOR
    val alignment = if (isCreator) Alignment.TopEnd else Alignment.TopStart
    val bubbleColor = if (isCreator) YralColors.Pink300 else YralColors.Neutral800
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = alignment,
    ) {
        Column(horizontalAlignment = if (isCreator) Alignment.End else Alignment.Start) {
            Box(
                modifier =
                    Modifier
                        .background(color = bubbleColor, shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (isCoachThinkingPlaceholder) {
                    Text(
                        text = stringResource(Res.string.coach_thinking),
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.NeutralTextSecondary,
                    )
                } else {
                    Text(
                        text = message.content,
                        style = LocalAppTopography.current.baseRegular,
                        color = if (isCreator) YralColors.Neutral0 else YralColors.NeutralTextPrimary,
                    )
                }
            }
            if (showProposalCard) {
                Spacer(modifier = Modifier.height(6.dp))
                CoachProposalCard(
                    proposedChanges = message.proposedChanges.orEmpty(),
                    reasoning = message.reasoning.orEmpty(),
                    applied = message.applied,
                    onApplyClick = onApplyClick,
                )
            }
        }
    }
}

@Composable
private fun CoachProposalCard(
    proposedChanges: String,
    reasoning: String,
    applied: Boolean,
    onApplyClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = YralColors.Pink300,
                    shape = RoundedCornerShape(12.dp),
                ).padding(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.coach_proposal_card_title),
            style = LocalAppTopography.current.smSemiBold,
            color = YralColors.Pink300,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (reasoning.isNotBlank()) {
            Text(
                text = reasoning,
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = proposedChanges,
            style = LocalAppTopography.current.regRegular,
            color = YralColors.NeutralTextPrimary,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (applied) YralColors.Neutral700 else YralColors.Pink300,
                        shape = RoundedCornerShape(20.dp),
                    ).clickable(enabled = !applied, onClick = onApplyClick)
                    .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    if (applied) {
                        stringResource(Res.string.coach_proposal_applied)
                    } else {
                        stringResource(Res.string.coach_proposal_apply_cta)
                    },
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.Neutral0,
            )
        }
    }
}

@Composable
private fun CoachSaveButton(
    botName: String?,
    enabled: Boolean,
    onSave: () -> Unit,
) {
    val label =
        if (!botName.isNullOrBlank()) {
            stringResource(Res.string.coach_save_cta, botName)
        } else {
            stringResource(Res.string.coach_save_cta_generic)
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .background(
                    color = if (enabled) YralColors.Pink300 else YralColors.Neutral700,
                    shape = RoundedCornerShape(24.dp),
                ).clickable(enabled = enabled, onClick = onSave)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.Neutral0,
        )
    }
}

@Composable
private fun CoachInputArea(
    onSend: (String) -> Unit,
    enabled: Boolean,
) {
    var input by remember { mutableStateOf("") }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .background(YralColors.Neutral900, RoundedCornerShape(30.dp))
                .border(1.dp, YralColors.Neutral800, RoundedCornerShape(30.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            modifier = Modifier.weight(1f),
            value = input,
            onValueChange = { input = it },
            textStyle = LocalAppTopography.current.baseRegular.copy(color = YralColors.NeutralTextPrimary),
            keyboardOptions = KeyboardOptions.Default,
            cursorBrush = SolidColor(YralColors.Pink300),
            maxLines = 5,
            decorationBox = { inner ->
                Box {
                    if (input.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.coach_input_placeholder),
                            color = YralColors.NeutralTextTertiary,
                            style = LocalAppTopography.current.baseRegular,
                        )
                    }
                    inner()
                }
            },
        )
        val sendEnabled = enabled && input.isNotBlank()
        Image(
            painter =
                painterResource(
                    if (sendEnabled) DesignRes.drawable.ic_send else DesignRes.drawable.ic_send_disabled,
                ),
            contentDescription = stringResource(Res.string.coach_send),
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(enabled = sendEnabled) {
                        val text = input.trim()
                        if (text.isNotEmpty()) {
                            onSend(text)
                            input = ""
                        }
                    },
        )
    }
}

@Composable
private fun CoachApplyConfirmDialog(
    botName: String?,
    isApplying: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isApplying) onDismiss() },
        title = { Text(stringResource(Res.string.coach_apply_confirm_title, botName.orEmpty())) },
        text = { Text(stringResource(Res.string.coach_apply_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isApplying) {
                Text(stringResource(Res.string.coach_apply_confirm_cta))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isApplying) {
                Text(stringResource(Res.string.coach_apply_confirm_cancel))
            }
        },
    )
}

@Composable
private fun CoachStartOverConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.coach_start_over_confirm_title)) },
        text = { Text(stringResource(Res.string.coach_start_over_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.coach_start_over_confirm_cta))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.coach_start_over_confirm_cancel))
            }
        },
    )
}

@Composable
private fun CoachErrorBanner(
    error: CoachError,
    onDismiss: () -> Unit,
) {
    val text =
        when (error) {
            is CoachError.SessionStartFailed -> stringResource(Res.string.coach_session_start_failed)
            is CoachError.SendFailed -> stringResource(Res.string.coach_send_failed)
            is CoachError.ApplyFailed -> stringResource(Res.string.coach_apply_failed)
        }
    LaunchedEffect(error) {
        ToastManager.showError(type = ToastType.Small(message = text))
        onDismiss()
    }
}
