package com.yral.shared.features.coach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.coach.domain.models.CoachMessage
import com.yral.shared.features.coach.domain.models.CoachMessageRole
import com.yral.shared.features.coach.domain.models.ProposalStatus
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
        sectionHint: String? = null,
    ) {
        startSession(
            botId = botId,
            botName = botName,
            avatarUrl = avatarUrl,
            fresh = false,
            sectionHint = sectionHint,
        )
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
        // Section hint intentionally NOT carried into start-over — a
        // brand-new session is creator-initiated, not a section tap.
        startSession(
            botId = botId,
            botName = current.botName,
            avatarUrl = current.avatarUrl,
            fresh = true,
            sectionHint = null,
        )
    }

    private fun startSession(
        botId: String,
        botName: String?,
        avatarUrl: String?,
        fresh: Boolean,
        sectionHint: String?,
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
                // PR-4 — fresh session has no proposal until the backend
                // says so; loadHistoryAndFinishLoading will set this from
                // the list-messages response.
                pendingProposalExists = false,
                // Correction C — entering a session is a clean slate;
                // don't carry a prior session's CTA dismissal into this one.
                postApplyCtasDismissed = false,
                isSessionLoading = true,
                error = null,
            )
        }
        viewModelScope.launch {
            createCoachSessionUseCase(
                CreateCoachSessionUseCase.Params(
                    botId = botId,
                    fresh = fresh,
                    sectionHint = sectionHint,
                ),
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
            .onSuccess { page ->
                _viewState.update {
                    // Guard against late responses after Start over.
                    if (it.coachConversationId != coachConversationId) {
                        it
                    } else {
                        it.copy(
                            pending = page.messages,
                            // PR-4 (2026-06-11) — session reload drives the
                            // Save button gate. Without this, returning to
                            // a conversation that has a pending proposal
                            // would show no Save button until the next send.
                            pendingProposalExists = page.pendingProposalExists,
                            isSessionLoading = false,
                        )
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
                // Per Rishi — once the creator types again, the
                // existing proposal's Apply hides. Only a fresh new
                // proposal in the reply re-opens an Apply somewhere
                // (on the new card).
                activeProposalLockedBySend = true,
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
                    // Per Rishi — clear the lock ONLY when Coach's
                    // reply itself is a new proposal. Otherwise the
                    // older proposal stays locked (no Apply
                    // re-appears) until the next send-and-propose
                    // cycle. hasProposal looks at the new message's
                    // proposed_changes.
                    val replyIsNewProposal = result.coachMessage.hasProposal
                    state.copy(
                        pending = mapped,
                        pendingCoachPlaceholderId = null,
                        isCoachThinking = false,
                        // PR-4 (2026-06-11) — backend-computed against
                        // post-turn state. Stays in sync with backend
                        // for any UI that wants to know "is something
                        // pending server-side."
                        pendingProposalExists = result.pendingProposalExists,
                        activeProposalLockedBySend = !replyIsNewProposal,
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
        val state = _viewState.value
        val convId = state.coachConversationId ?: return
        // PR-3 (#356) — pass the specific proposal id the user is acting
        // on. Today's UI only ever surfaces the latest unapplied proposal,
        // so `activeProposalMessage.id` is the natural pick (matches the
        // visible card). If we later add per-card Apply (e.g. scroll-up
        // re-apply), this should switch to the tapped card's id instead.
        // Guard: if there's no pending proposal somehow, bail rather than
        // send an empty id (backend would 422 us).
        val proposalId =
            state.activeProposalMessage?.id ?: run {
                Logger.e("CoachViewModel") { "confirmApplyProposal: no activeProposalMessage to apply" }
                return
            }
        _viewState.update { it.copy(isApplying = true, showApplyConfirm = false, error = null) }
        viewModelScope.launch {
            applyCoachProposalUseCase(
                ApplyCoachProposalUseCase.Params(
                    coachConversationId = convId,
                    proposalId = proposalId,
                ),
            ).onSuccess { result ->
                    _viewState.update { state ->
                        // Mark latest proposal as applied (ProposalCard becomes
                        // inert), then append the backend-issued receipt
                        // message so the creator sees a "✅ Saved" record
                        // without needing to re-list.
                        // Mark the proposal we just applied as APPLIED
                        // status (post-PR-3 the lifecycle is the source
                        // of truth) and keep the legacy `applied` bool
                        // synced for any callers still reading it.
                        // Append the backend-issued receipt as the most
                        // recent message — flagged `isReceipt=true` so
                        // the screen renders it distinctly and shows
                        // the post-apply CTA pair below it.
                        val updatedPending =
                            state.pending
                                .map { msg ->
                                    if (msg.id == proposalId) {
                                        msg.copy(
                                            status = ProposalStatus.APPLIED,
                                            applied = true,
                                        )
                                    } else {
                                        msg
                                    }
                                }.let { withApplied ->
                                    result.receiptMessage
                                        ?.copy(isReceipt = true)
                                        ?.let { receipt -> withApplied + receipt }
                                        ?: withApplied
                                }
                        state.copy(
                            pending = updatedPending,
                            isApplying = false,
                            lastAppliedToastMessage = "Updated ${state.botName ?: "your AI Influencer"}'s personality.",
                            // PR-4 — apply succeeded, no more pending
                            // proposal until the creator chats more.
                            pendingProposalExists = false,
                            // Correction C — fresh receipt + dismiss
                            // flag reset, so the CTA pair shows below
                            // it until the user taps Continue.
                            postApplyCtasDismissed = false,
                            // Apply was just done — the lock is moot
                            // because there's nothing pending anyway.
                            activeProposalLockedBySend = false,
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

    /**
     * Correction C — creator tapped "Continue coaching" on the
     * post-apply CTA pair. Just hides the CTAs; the input area stays
     * usable and the chat stays put. The CTAs only re-appear when a
     * fresh receipt arrives via [confirmApplyProposal].
     */
    fun dismissPostApplyCtas() {
        _viewState.update { it.copy(postApplyCtasDismissed = true) }
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
    /**
     * PR-4 (2026-06-11) — backend-computed flag from message-list and
     * send-message responses. Drives the Save-button gate: button is
     * hidden when false so the creator can't tap Save with nothing to
     * save (which used to trigger a "mystery LLM round-trip").
     */
    val pendingProposalExists: Boolean = false,
    /**
     * Correction C — transient flag flipped TRUE when the creator taps
     * "Continue coaching" on the post-apply CTA pair. Reset to FALSE
     * when a fresh apply happens (so the CTAs come back for the next
     * apply). Never persisted — re-entering Coach starts as FALSE.
     */
    val postApplyCtasDismissed: Boolean = false,
    /**
     * Per Rishi 2026-06-12 evening — the moment the creator starts
     * typing again, the existing proposal's Apply becomes invisible.
     * It only re-appears if Coach's NEXT reply carries a brand-new
     * proposal. If Coach replies without a proposal, the older one
     * stays disabled forever (forcing the creator to ask Coach to
     * formalize the new idea before they can apply). This is mobile-
     * local; backend should eventually mirror with supersede-on-send
     * — flagged to Session 6.
     */
    val activeProposalLockedBySend: Boolean = false,
    val error: CoachError? = null,
) {
    val activeProposalMessage: CoachMessage?
        get() = pending.lastOrNull { it.hasProposal && !it.applied }

    /**
     * Correction C — show the "Continue coaching / I'm done for now"
     * CTA pair below the latest message when the most recent message
     * is a receipt AND there's nothing new to save AND the creator
     * hasn't already dismissed the CTAs from this apply.
     */
    val showPostApplyCtaPair: Boolean
        get() =
            !postApplyCtasDismissed &&
                !pendingProposalExists &&
                pending.lastOrNull()?.isReceipt == true

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

}

sealed class CoachError {
    abstract val message: String

    data class SessionStartFailed(
        override val message: String,
    ) : CoachError()
    data class SendFailed(
        override val message: String,
    ) : CoachError()
    data class ApplyFailed(
        override val message: String,
    ) : CoachError()
}
