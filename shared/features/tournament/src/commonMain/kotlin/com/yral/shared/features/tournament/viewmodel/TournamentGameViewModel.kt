package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.tournament.domain.CastTournamentVoteUseCase
import com.yral.shared.features.tournament.domain.model.CastTournamentVoteRequest
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import com.yral.shared.features.tournament.domain.model.VoteOutcome
import com.yral.shared.features.tournament.domain.model.VoteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TournamentGameViewModel(
    private val sessionManager: SessionManager,
    private val gameIconsUseCase: GetGameIconsUseCase,
    private val castTournamentVoteUseCase: CastTournamentVoteUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(TournamentGameState())
    val state: StateFlow<TournamentGameState> = _state.asStateFlow()

    init {
        viewModelScope.launch { getGameIcons() }
    }

    fun setTournament(
        tournamentId: String,
        initialDiamonds: Int,
        endEpochMs: Long,
    ) {
        _state.update {
            it.copy(
                tournamentId = tournamentId,
                diamonds = initialDiamonds,
                endEpochMs = endEpochMs,
            )
        }
    }

    private suspend fun getGameIcons() {
        gameIconsUseCase
            .invoke(Unit)
            .onSuccess { config ->
                _state.update { it.copy(gameIcons = config.availableSmileys) }
            }.onFailure { }
    }

    fun setClickedIcon(
        icon: GameIcon,
        feedDetails: FeedDetails,
    ) {
        val currentState = _state.value
        if (currentState.isLoading) return
        if (currentState.diamonds <= 0) {
            _state.update { it.copy(noDiamondsError = true) }
            return
        }
        viewModelScope.launch { castVote(icon, feedDetails) }
    }

    private suspend fun castVote(
        icon: GameIcon,
        feedDetails: FeedDetails,
    ) {
        val currentState = _state.value
        val principalId = sessionManager.userPrincipal ?: return

        _state.update { it.copy(isLoading = true) }

        castTournamentVoteUseCase
            .invoke(
                CastTournamentVoteRequest(
                    tournamentId = currentState.tournamentId,
                    principalId = principalId,
                    videoId = feedDetails.videoID,
                    smileyId = icon.id,
                ),
            ).onSuccess { result ->
                val diamondDelta = if (result.outcome == VoteOutcome.WIN) 1 else -1
                _state.update {
                    val updatedResults = it.voteResults.toMutableMap()
                    updatedResults[feedDetails.videoID] = result
                    it.copy(
                        isLoading = false,
                        diamonds = result.diamonds,
                        position = result.position,
                        wins = result.tournamentWins,
                        losses = result.tournamentLosses,
                        lastVoteOutcome = result.outcome,
                        lastDiamondDelta = diamondDelta,
                        voteResults = updatedResults,
                        lastVotedCount = it.lastVotedCount + 1,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        noDiamondsError = error.code == TournamentErrorCodes.NO_DIAMONDS,
                        tournamentEndedError = error.code == TournamentErrorCodes.TOURNAMENT_NOT_LIVE,
                    )
                }
            }
    }

    fun hasVotedOnVideo(videoId: String): Boolean = _state.value.voteResults.containsKey(videoId)

    fun getVoteResult(videoId: String): VoteResult? = _state.value.voteResults[videoId]

    fun hasShownCoinDeltaAnimation(videoId: String): Boolean =
        _state.value.shownCoinDeltaAnimations.contains(videoId)

    fun markCoinDeltaAnimationShown(videoId: String) {
        _state.update {
            if (it.shownCoinDeltaAnimations.contains(videoId)) {
                it
            } else {
                it.copy(shownCoinDeltaAnimations = it.shownCoinDeltaAnimations + videoId)
            }
        }
    }

    fun setCurrentVideoId(videoId: String) {
        _state.update { it.copy(currentVideoId = videoId) }
    }

    fun clearNoDiamondsError() {
        _state.update { it.copy(noDiamondsError = false) }
    }

    fun clearTournamentEndedError() {
        _state.update { it.copy(tournamentEndedError = false) }
    }

    fun clearLastVoteOutcome() {
        _state.update { it.copy(lastVoteOutcome = null, lastDiamondDelta = 0) }
    }
}

data class TournamentGameState(
    val tournamentId: String = "",
    val gameIcons: List<GameIcon> = emptyList(),
    val diamonds: Int = 0,
    val position: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val endEpochMs: Long = 0,
    val isLoading: Boolean = false,
    val currentVideoId: String = "",
    val lastVoteOutcome: VoteOutcome? = null,
    val lastDiamondDelta: Int = 0,
    val voteResults: Map<String, VoteResult> = emptyMap(),
    val shownCoinDeltaAnimations: Set<String> = emptySet(),
    val lastVotedCount: Int = 1,
    val noDiamondsError: Boolean = false,
    val tournamentEndedError: Boolean = false,
)
