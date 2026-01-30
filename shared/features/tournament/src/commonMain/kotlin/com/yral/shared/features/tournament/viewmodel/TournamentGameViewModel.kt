package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
import com.yral.shared.features.tournament.analytics.TournamentTelemetry
import com.yral.shared.features.tournament.domain.CastHotOrNotVoteUseCase
import com.yral.shared.features.tournament.domain.CastTournamentVoteUseCase
import com.yral.shared.features.tournament.domain.GetTournamentsUseCase
import com.yral.shared.features.tournament.domain.GetVideoEmojisRequest
import com.yral.shared.features.tournament.domain.GetVideoEmojisUseCase
import com.yral.shared.features.tournament.domain.model.CastTournamentVoteRequest
import com.yral.shared.features.tournament.domain.model.GetTournamentsRequest
import com.yral.shared.features.tournament.domain.model.HotOrNotVoteRequest
import com.yral.shared.features.tournament.domain.model.HotOrNotVoteResult
import com.yral.shared.features.tournament.domain.model.TournamentError
import com.yral.shared.features.tournament.domain.model.TournamentErrorCodes
import com.yral.shared.features.tournament.domain.model.TournamentType
import com.yral.shared.features.tournament.domain.model.VideoEmoji
import com.yral.shared.features.tournament.domain.model.VoteOutcome
import com.yral.shared.features.tournament.domain.model.VoteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class TournamentGameViewModel(
    private val sessionManager: SessionManager,
    private val gameIconsUseCase: GetGameIconsUseCase,
    private val castTournamentVoteUseCase: CastTournamentVoteUseCase,
    private val castHotOrNotVoteUseCase: CastHotOrNotVoteUseCase,
    private val getTournamentsUseCase: GetTournamentsUseCase,
    private val getVideoEmojisUseCase: GetVideoEmojisUseCase,
    private val telemetry: TournamentTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(TournamentGameState())
    val state: StateFlow<TournamentGameState> = _state.asStateFlow()

    // Separate StateFlow for video emojis to avoid triggering main state recompositions
    // when prefetching emojis for videos during scroll
    private val _videoEmojisState = MutableStateFlow<Map<String, List<GameIcon>>>(emptyMap())
    val videoEmojisState: StateFlow<Map<String, List<GameIcon>>> = _videoEmojisState.asStateFlow()

    // Track loading state outside of StateFlow to avoid recompositions during scroll
    private val loadingVideoEmojis = mutableSetOf<String>()

    init {
        viewModelScope.launch { getGameIcons() }
    }

    fun setTournament(
        tournamentId: String,
        tournamentType: TournamentType,
        initialDiamonds: Int,
        endEpochMs: Long,
    ) {
        val currentState = _state.value
        // If already set up for this tournament, just refresh diamonds from API
        if (currentState.tournamentId == tournamentId && currentState.diamonds > 0) {
            // Refresh diamonds from API to get current balance
            refreshDiamondsFromApi(tournamentId)
            return
        }

        // First time setup - use initial diamonds then refresh from API
        _state.update {
            it.copy(
                tournamentId = tournamentId,
                tournamentType = tournamentType,
                diamonds = initialDiamonds,
                endEpochMs = endEpochMs,
            )
        }

        // Track tournament joined
        telemetry.onTournamentJoined(
            tournamentId = tournamentId,
            tournamentType = tournamentType,
            diamondsAllocated = initialDiamonds,
        )

        // Fetch fresh diamond balance from API
        refreshDiamondsFromApi(tournamentId)
    }

    private fun refreshDiamondsFromApi(tournamentId: String) {
        val principalId = sessionManager.userPrincipal ?: return
        viewModelScope.launch {
            getTournamentsUseCase
                .invoke(
                    GetTournamentsRequest(
                        tournamentId = tournamentId,
                        principalId = principalId,
                    ),
                ).onSuccess { tournaments ->
                    val tournament = tournaments.firstOrNull() ?: return@onSuccess
                    val userStats = tournament.userStats ?: return@onSuccess
                    val hasPlayed = (userStats.tournamentWins + userStats.tournamentLosses) > 0
                    _state.update {
                        it.copy(
                            diamonds = userStats.diamonds,
                            hasPlayedBefore = hasPlayed,
                            activeParticipantCount = tournament.participantCount,
                        )
                    }
                }
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
            // Track out of diamonds nudge shown
            telemetry.onOutOfDiamondsShown(
                tournamentId = currentState.tournamentId,
                tournamentType = currentState.tournamentType,
            )
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
                handleVoteSuccess(result, currentState, feedDetails.videoID)
            }.onFailure { error ->
                handleVoteFailure(error)
            }
    }

    private fun handleVoteSuccess(
        result: VoteResult,
        currentState: TournamentGameState,
        videoId: String,
    ) {
        val diamondDelta = result.diamondDelta ?: (result.diamonds - currentState.diamonds)
        val resolvedResult = if (result.diamondDelta == null) result.copy(diamondDelta = diamondDelta) else result

        telemetry.onAnswerSubmitted(
            tournamentId = currentState.tournamentId,
            tournamentType = currentState.tournamentType,
            isCorrect = result.outcome == VoteOutcome.WIN,
            scoreDelta = diamondDelta,
            diamondsRemaining = result.diamonds,
        )

        // Update video emojis in dedicated StateFlow (separate from main state)
        val videoIcons = result.videoEmojis?.map { it.toGameIcon() }
        if (!videoIcons.isNullOrEmpty()) {
            _videoEmojisState.update { it + (videoId to videoIcons) }
        }

        _state.update {
            val updatedResults = it.voteResults.toMutableMap().apply { put(videoId, resolvedResult) }

            it.copy(
                isLoading = false,
                diamonds = result.diamonds,
                position = result.position,
                activeParticipantCount = result.activeParticipantCount,
                wins = result.tournamentWins,
                losses = result.tournamentLosses,
                lastVoteOutcome = result.outcome,
                lastDiamondDelta = diamondDelta,
                voteResults = updatedResults,
                lastVotedCount = it.lastVotedCount + 1,
            )
        }
    }

    private fun handleVoteFailure(error: TournamentError) {
        _state.update {
            it.copy(
                isLoading = false,
                noDiamondsError = error.code == TournamentErrorCodes.NO_DIAMONDS,
                tournamentEndedError = error.code == TournamentErrorCodes.TOURNAMENT_NOT_LIVE,
            )
        }
    }

    /**
     * Cast a hot or not vote based on swipe direction.
     * @param isHot true for right swipe (hot), false for left swipe (not)
     * @param videoId the video ID being voted on
     */
    fun castSwipeVote(
        isHot: Boolean,
        videoId: String,
    ) {
        val currentState = _state.value
        if (currentState.isHotOrNotVoting) return
        if (currentState.diamonds <= 0) {
            _state.update { it.copy(noDiamondsError = true) }
            telemetry.onOutOfDiamondsShown(
                tournamentId = currentState.tournamentId,
                tournamentType = currentState.tournamentType,
            )
            return
        }
        viewModelScope.launch { performHotOrNotVote(isHot, videoId) }
    }

    private suspend fun performHotOrNotVote(
        isHot: Boolean,
        videoId: String,
    ) {
        val currentState = _state.value
        val principalId = sessionManager.userPrincipal ?: return
        val vote = if (isHot) "hot" else "not"

        _state.update { it.copy(isHotOrNotVoting = true) }

        castHotOrNotVoteUseCase
            .invoke(
                HotOrNotVoteRequest(
                    tournamentId = currentState.tournamentId,
                    principalId = principalId,
                    videoId = videoId,
                    vote = vote,
                ),
            ).onSuccess { result ->
                // Track answer submitted
                val isCorrect = result.outcome == "WIN"
                telemetry.onAnswerSubmitted(
                    tournamentId = currentState.tournamentId,
                    tournamentType = currentState.tournamentType,
                    isCorrect = isCorrect,
                    scoreDelta = result.diamondDelta,
                    diamondsRemaining = result.diamonds,
                )

                val outcome = if (result.outcome == "WIN") VoteOutcome.WIN else VoteOutcome.LOSS

                _state.update {
                    val updatedResults = it.hotOrNotVoteResults.toMutableMap()
                    updatedResults[videoId] = result
                    it.copy(
                        isHotOrNotVoting = false,
                        diamonds = result.diamonds,
                        position = result.position,
                        activeParticipantCount = result.activeParticipantCount,
                        wins = result.wins,
                        losses = result.losses,
                        lastVoteOutcome = outcome,
                        lastDiamondDelta = result.diamondDelta,
                        hotOrNotVoteResults = updatedResults,
                        lastVotedCount = it.lastVotedCount + 1,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isHotOrNotVoting = false,
                        noDiamondsError = error.code == TournamentErrorCodes.NO_DIAMONDS,
                        tournamentEndedError = error.code == TournamentErrorCodes.TOURNAMENT_NOT_LIVE,
                    )
                }
            }
    }

    fun hasVotedOnVideo(videoId: String): Boolean = _state.value.voteResults.containsKey(videoId)

    fun hasVotedOnHotOrNotVideo(videoId: String): Boolean = _state.value.hotOrNotVoteResults.containsKey(videoId)

    /**
     * Get emojis for a specific video.
     * Returns video-specific emojis if available (from Gemini analysis).
     * Returns empty list while loading to prevent showing fallback emojis.
     * Falls back to global game icons only if loading failed.
     */
    fun getIconsForVideo(videoId: String): List<GameIcon> {
        val videoEmojis = _videoEmojisState.value
        return videoEmojis[videoId]
            ?: if (loadingVideoEmojis.contains(videoId)) emptyList() else _state.value.gameIcons
    }

    fun getVoteResult(videoId: String): VoteResult? = _state.value.voteResults[videoId]

    fun getHotOrNotVoteResult(videoId: String): HotOrNotVoteResult? = _state.value.hotOrNotVoteResults[videoId]

    fun hasShownCoinDeltaAnimation(videoId: String): Boolean = _state.value.shownCoinDeltaAnimations.contains(videoId)

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
        // Prefetch emojis for this video if not already cached
        prefetchVideoEmojis(videoId)
    }

    /**
     * Prefetch video-specific emojis from the backend.
     * This is called when a video becomes visible to ensure
     * the correct emojis are shown before the user votes.
     */
    fun prefetchVideoEmojis(videoId: String) {
        val currentState = _state.value
        // Skip if already cached, already loading, or if tournament ID not set
        val shouldSkip =
            _videoEmojisState.value.containsKey(videoId) ||
                loadingVideoEmojis.contains(videoId) ||
                currentState.tournamentId.isEmpty()
        if (shouldSkip) return

        // Mark as loading (no state update to avoid recomposition)
        loadingVideoEmojis.add(videoId)

        viewModelScope.launch {
            getVideoEmojisUseCase
                .invoke(
                    GetVideoEmojisRequest(
                        tournamentId = currentState.tournamentId,
                        videoId = videoId,
                    ),
                ).onSuccess { result ->
                    loadingVideoEmojis.remove(videoId)
                    val videoIcons = result.emojis.map { emoji -> emoji.toGameIcon() }
                    // Update dedicated StateFlow to avoid main state recompositions during scroll
                    _videoEmojisState.update { it + (videoId to videoIcons) }
                }.onFailure {
                    // Remove from loading - will use global icons as fallback
                    loadingVideoEmojis.remove(videoId)
                }
        }
    }

    fun clearNoDiamondsError() {
        _state.update { it.copy(noDiamondsError = false) }
    }

    fun clearTournamentEndedError() {
        _state.update { it.copy(tournamentEndedError = false) }
    }

    // Telemetry tracking methods
    fun trackExitAttempted() {
        val currentState = _state.value
        telemetry.onExitAttempted(
            tournamentId = currentState.tournamentId,
            tournamentType = currentState.tournamentType,
            diamondsRemaining = currentState.diamonds,
        )
    }

    fun trackExitNudgeShown() {
        val currentState = _state.value
        telemetry.onExitNudgeShown(
            tournamentId = currentState.tournamentId,
            tournamentType = currentState.tournamentType,
        )
    }

    fun trackExitConfirmed() {
        val currentState = _state.value
        telemetry.onExitConfirmed(
            tournamentId = currentState.tournamentId,
            tournamentType = currentState.tournamentType,
            diamondsRemaining = currentState.diamonds,
        )
    }

    fun trackTournamentEnded(tournamentName: String) {
        val currentState = _state.value
        telemetry.onTournamentEnded(
            tournamentId = currentState.tournamentId,
            tournamentType = currentState.tournamentType,
            tournamentName = tournamentName,
        )
    }
}

data class TournamentGameState(
    val tournamentId: String = "",
    val tournamentType: TournamentType = TournamentType.SMILEY,
    val gameIcons: List<GameIcon> = emptyList(),
    val videoEmojis: Map<String, List<GameIcon>> = emptyMap(),
    val diamonds: Int = 0,
    val position: Int = 0,
    val activeParticipantCount: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val endEpochMs: Long = 0,
    val isLoading: Boolean = false,
    val isHotOrNotVoting: Boolean = false,
    val currentVideoId: String = "",
    val lastVoteOutcome: VoteOutcome? = null,
    val lastDiamondDelta: Int = 0,
    val voteResults: Map<String, VoteResult> = emptyMap(),
    val hotOrNotVoteResults: Map<String, HotOrNotVoteResult> = emptyMap(),
    val shownCoinDeltaAnimations: Set<String> = emptySet(),
    val lastVotedCount: Int = 1,
    val noDiamondsError: Boolean = false,
    val tournamentEndedError: Boolean = false,
    val hasPlayedBefore: Boolean = false,
)

/**
 * Convert VideoEmoji (from Gemini analysis) to GameIcon for UI rendering.
 * Uses UNKNOWN for imageName since dynamic emojis don't have predefined types.
 * The unicode field enables rendering via text fallback.
 */
fun VideoEmoji.toGameIcon(): GameIcon =
    GameIcon(
        id = id,
        imageName = GameIconNames.UNKNOWN,
        imageUrl = "",
        clickAnimation = "",
        unicode = unicode,
    )
