package com.yral.shared.features.coach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.coach.domain.models.CoachMessage
import com.yral.shared.features.coach.domain.models.CoachMessageRole
import com.yral.shared.features.coach.domain.usecases.ApplyCoachProposalUseCase
import com.yral.shared.features.coach.domain.usecases.CreateCoachSessionUseCase
import com.yral.shared.features.coach.domain.usecases.ListCoachMessagesUseCase
import com.yral.shared.features.coach.domain.usecases.SendCoachMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CoachViewModel(
    private val createCoachSessionUseCase: CreateCoachSessionUseCase,
    private val sendCoachMessageUseCase: SendCoachMessageUseCase,
    private val applyCoachProposalUseCase: ApplyCoachProposalUseCase,
    private val listCoachMessagesUseCase: ListCoachMessagesUseCase,
) : ViewModel() {
    private val _viewState = MutableStateFlow(CoachViewState())
    val viewState: StateFlow<CoachViewState> = _viewState.asStateFlow()

    /**
     * Start a fresh coach session for the given bot. Mobile flow:
     * tap "Make your AI Influencer better" → ProfileViewModel calls this
     * → coach screen navigates with the returned conversation id.
     *
     * v1 always creates a NEW session per tap. Resuming the most recent
     * session is a future polish item.
     */
    fun openForBot(
        botId: String,
        botName: String?,
        avatarUrl: String?,
    ) {
        _viewState.update {
            it.copy(
                botId = botId,
                botName = botName,
                avatarUrl = avatarUrl,
                coachConversationId = null,
                pending = emptyList(),
                pendingCoachPlaceholderId = null,
                isCoachThinking = false,
                isApplying = false,
                showApplyConfirm = false,
                lastAppliedToastMessage = null,
                isSessionLoading = true,
                error = null,
            )
        }
        viewModelScope.launch {
            createCoachSessionUseCase(botId)
                .onSuccess { session ->
                    _viewState.update {
                        it.copy(
                            coachConversationId = session.id,
                            isSessionLoading = false,
                            // Backend may have populated bot_name; prefer that when present
                            botName = session.botName ?: it.botName,
                        )
                    }
                }.onFailure { error ->
                    Logger.e(error) { "Coach createSession failed for bot=$botId" }
                    _viewState.update {
                        it.copy(
                            isSessionLoading = false,
                            error = CoachError.SessionStartFailed(error.message ?: "Could not start coach session"),
                        )
                    }
                }
        }
    }

    fun sendMessage(text: String) {
        val convId = _viewState.value.coachConversationId ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // Optimistic local creator bubble + "coach is thinking" placeholder.
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val localCreatorId = "local-creator-$nowMs"
        val localCoachId = "local-coach-$nowMs"

        val creatorLocal =
            CoachMessage(
                id = localCreatorId,
                coachConversationId = convId,
                role = CoachMessageRole.CREATOR,
                content = trimmed,
                createdAt = nowMs.toString(),
            )
        val coachPlaceholder =
            CoachMessage(
                id = localCoachId,
                coachConversationId = convId,
                role = CoachMessageRole.COACH,
                content = "",
                createdAt = nowMs.toString(),
            )

        _viewState.update {
            it.copy(
                pending = it.pending + creatorLocal + coachPlaceholder,
                pendingCoachPlaceholderId = localCoachId,
                isCoachThinking = true,
            )
        }

        viewModelScope.launch {
            sendCoachMessageUseCase(
                SendCoachMessageUseCase.Params(coachConversationId = convId, content = trimmed),
            ).onSuccess { result ->
                _viewState.update { state ->
                    // Swap optimistic creator local for the server-confirmed one;
                    // replace the placeholder with the real coach message.
                    val mapped =
                        state.pending.mapNotNull { msg ->
                            when (msg.id) {
                                localCreatorId -> result.creatorMessage
                                localCoachId -> result.coachMessage
                                else -> msg
                            }
                        }
                    state.copy(
                        pending = mapped,
                        pendingCoachPlaceholderId = null,
                        isCoachThinking = false,
                    )
                }
            }.onFailure { error ->
                Logger.e(error) { "Coach sendMessage failed convId=$convId" }
                _viewState.update { state ->
                    // Drop the placeholder; keep the creator local with a "failed" flag
                    // (v1 doesn't render retry — user re-types).
                    state.copy(
                        pending = state.pending.filterNot { it.id == localCoachId },
                        pendingCoachPlaceholderId = null,
                        isCoachThinking = false,
                        error = CoachError.SendFailed(error.message ?: "Coach reply failed"),
                    )
                }
            }
        }
    }

    fun confirmApplyProposal() {
        val convId = _viewState.value.coachConversationId ?: return
        _viewState.update { it.copy(isApplying = true, showApplyConfirm = false, error = null) }
        viewModelScope.launch {
            applyCoachProposalUseCase(convId)
                .onSuccess { result ->
                    _viewState.update { state ->
                        // Mark the latest proposal-carrying message as applied so
                        // the ProposalCard becomes inert + shows "Applied" state.
                        val updated =
                            state.pending.map { msg ->
                                if (msg.hasProposal && !msg.applied) {
                                    msg.copy(applied = true)
                                } else {
                                    msg
                                }
                            }
                        state.copy(
                            pending = updated,
                            isApplying = false,
                            lastAppliedToastMessage = "Updated ${state.botName ?: "your AI Influencer"}'s personality.",
                        )
                    }
                }.onFailure { error ->
                    Logger.e(error) { "Coach applyProposal failed convId=$convId" }
                    _viewState.update {
                        it.copy(
                            isApplying = false,
                            error = CoachError.ApplyFailed(error.message ?: "Could not apply the proposal"),
                        )
                    }
                }
        }
    }

    fun requestApplyProposal() {
        _viewState.update { it.copy(showApplyConfirm = true) }
    }

    fun dismissApplyConfirm() {
        _viewState.update { it.copy(showApplyConfirm = false) }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    fun clearAppliedToast() {
        _viewState.update { it.copy(lastAppliedToastMessage = null) }
    }
}

data class CoachViewState(
    val botId: String? = null,
    val botName: String? = null,
    val avatarUrl: String? = null,
    val coachConversationId: String? = null,
    val isSessionLoading: Boolean = false,
    val pending: List<CoachMessage> = emptyList(),
    val pendingCoachPlaceholderId: String? = null,
    val isCoachThinking: Boolean = false,
    val showApplyConfirm: Boolean = false,
    val isApplying: Boolean = false,
    val lastAppliedToastMessage: String? = null,
    val error: CoachError? = null,
) {
    /** Latest message that carries a non-applied proposal. Renders the ProposalCard below. */
    val activeProposalMessage: CoachMessage?
        get() = pending.lastOrNull { it.hasProposal && !it.applied }
}

sealed class CoachError {
    abstract val message: String

    data class SessionStartFailed(override val message: String) : CoachError()
    data class SendFailed(override val message: String) : CoachError()
    data class ApplyFailed(override val message: String) : CoachError()
}
