package com.yral.shared.features.coach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
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
     * Open the coach for the given bot. Default is RESUME — the backend
     * returns the most-recent existing session and we fetch its history
     * via [listCoachMessagesUseCase] so the creator picks up where they
     * left off. Use [startOver] to force a brand-new conversation.
     */
    fun openForBot(
        botId: String,
        botName: String?,
        avatarUrl: String?,
    ) {
        startSession(botId = botId, botName = botName, avatarUrl = avatarUrl, fresh = false)
    }

    /**
     * Header "Start over" — discards the current session view and asks the
     * backend for a brand-new conversation (fresh=true). The previous
     * session is not deleted server-side; it just stops being the
     * most-recent for the creator+bot pair.
     */
    fun startOver() {
        val current = _viewState.value
        val botId = current.botId ?: return
        startSession(botId = botId, botName = current.botName, avatarUrl = current.avatarUrl, fresh = true)
    }

    private fun startSession(
        botId: String,
        botName: String?,
        avatarUrl: String?,
        fresh: Boolean,
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
                showStartOverConfirm = false,
                lastAppliedToastMessage = null,
                isSessionLoading = true,
                error = null,
            )
        }
        viewModelScope.launch {
            createCoachSessionUseCase(
                CreateCoachSessionUseCase.Params(botId = botId, fresh = fresh),
            ).onSuccess { session ->
                _viewState.update {
                    it.copy(
                        coachConversationId = session.id,
                        botName = session.botName ?: it.botName,
                    )
                }
                // Whether resumed or newly created, the server already holds
                // the messages we should render — existing turns for resume,
                // or the coach's opening greeting + suggestion chips for
                // fresh. One GET /messages call materialises both.
                loadHistoryAndFinishLoading(session.id)
            }.onFailure { error ->
                Logger.e(error) { "Coach createSession failed for bot=$botId fresh=$fresh" }
                _viewState.update {
                    it.copy(
                        isSessionLoading = false,
                        error = CoachError.SessionStartFailed(error.message ?: "Could not start coach session"),
                    )
                }
            }
        }
    }

    private suspend fun loadHistoryAndFinishLoading(coachConversationId: String) {
        listCoachMessagesUseCase(coachConversationId)
            .onSuccess { messages ->
                _viewState.update {
                    // Guard against late responses after Start over.
                    if (it.coachConversationId != coachConversationId) {
                        it
                    } else {
                        it.copy(pending = messages, isSessionLoading = false)
                    }
                }
            }.onFailure { error ->
                Logger.e(error) { "Coach listMessages failed convId=$coachConversationId" }
                _viewState.update { it.copy(isSessionLoading = false) }
            }
    }

    fun sendMessage(text: String) {
        sendInternal(content = text, requestProposal = false)
    }

    /**
     * "Save changes to {bot}" — sends the accumulated coaching intent and
     * asks the backend to force a structured proposal (request_proposal=true).
     * The coach reply will populate proposedChanges, the existing ProposalCard
     * renders, and the Apply confirm flow takes over unchanged.
     */
    fun requestSaveProposal() {
        sendInternal(content = SAVE_PROMPT_TEXT, requestProposal = true)
    }

    private fun sendInternal(
        content: String,
        requestProposal: Boolean,
    ) {
        val convId = _viewState.value.coachConversationId ?: return
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

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
                SendCoachMessageUseCase.Params(
                    coachConversationId = convId,
                    content = trimmed,
                    requestProposal = requestProposal,
                ),
            ).onSuccess { result ->
                _viewState.update { state ->
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
                Logger.e(error) { "Coach sendMessage failed convId=$convId requestProposal=$requestProposal" }
                _viewState.update { state ->
                    state.copy(
                        pending = state.pending.filterNot { it.id == localCreatorId || it.id == localCoachId },
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
                        // Mark latest proposal as applied (ProposalCard becomes
                        // inert), then append the backend-issued receipt
                        // message so the creator sees a "✅ Saved" record
                        // without needing to re-list.
                        val updatedPending =
                            state.pending
                                .map { msg ->
                                    if (msg.hasProposal && !msg.applied) msg.copy(applied = true) else msg
                                }.let { withApplied ->
                                    result.receiptMessage?.let { receipt -> withApplied + receipt } ?: withApplied
                                }
                        state.copy(
                            pending = updatedPending,
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

    fun requestStartOverConfirm() {
        _viewState.update { it.copy(showStartOverConfirm = true) }
    }

    fun dismissStartOverConfirm() {
        _viewState.update { it.copy(showStartOverConfirm = false) }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }

    fun clearAppliedToast() {
        _viewState.update { it.copy(lastAppliedToastMessage = null) }
    }

    private companion object {
        const val SAVE_PROMPT_TEXT = "Save these changes."
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
    val showStartOverConfirm: Boolean = false,
    val isApplying: Boolean = false,
    val lastAppliedToastMessage: String? = null,
    val error: CoachError? = null,
) {
    val activeProposalMessage: CoachMessage?
        get() = pending.lastOrNull { it.hasProposal && !it.applied }

    /**
     * Opening suggestion chips — drawn from the first coach message ONLY
     * while no creator has typed. As soon as the creator sends one, the
     * chips clear so they don't compete with the live conversation.
     */
    val openingSuggestions: List<String>
        get() {
            if (pending.any { it.role == CoachMessageRole.CREATOR }) return emptyList()
            val firstCoach = pending.firstOrNull { it.role == CoachMessageRole.COACH }
            return firstCoach?.suggestions.orEmpty()
        }

    val canRequestSave: Boolean
        get() = pending.any { it.role == CoachMessageRole.CREATOR }
}

sealed class CoachError {
    abstract val message: String

    data class SessionStartFailed(override val message: String) : CoachError()
    data class SendFailed(override val message: String) : CoachError()
    data class ApplyFailed(override val message: String) : CoachError()
}
