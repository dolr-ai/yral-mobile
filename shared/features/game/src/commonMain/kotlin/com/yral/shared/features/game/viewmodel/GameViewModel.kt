package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.events.GameConcludedCtaType
import com.yral.shared.analytics.events.GameType
import com.yral.shared.core.session.DELAY_FOR_SESSION_PROPERTIES
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.game.analytics.GameTelemetry
import com.yral.shared.features.game.domain.AutoRechargeBalanceUseCase
import com.yral.shared.features.game.domain.CastVoteUseCase
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.GetGameRulesUseCase
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceRequest
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.VoteResult
import com.yral.shared.features.game.domain.models.toVoteResult
import com.yral.shared.features.game.viewmodel.GameViewModel.Companion.SHOW_HOW_TO_PLAY_MAX_PAGE
import com.yral.shared.libs.designsystem.component.toast.ToastDuration
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showWarning
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class GameViewModel(
    private val preferences: Preferences,
    private val sessionManager: SessionManager,
    private val gameIconsUseCase: GetGameIconsUseCase,
    private val gameRulesUseCase: GetGameRulesUseCase,
    private val castVoteUseCase: CastVoteUseCase,
    private val gameTelemetry: GameTelemetry,
    private val autoRechargeBalanceUseCase: AutoRechargeBalanceUseCase,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            GameState(
                gameIcons = emptyList(),
                gameRules = emptyList(),
                coinBalance = 0,
            ),
        )
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        viewModelScope.launch { restoreDataFromPrefs() }
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.coinBalance },
                    defaultValue = 0,
                ).collect { coinBalance ->
                    Logger.d("coinBalance") { "coin balance collected $coinBalance" }
                    _state.update { it.copy(coinBalance = coinBalance) }
                }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionProperty {
                    it.isFirebaseLoggedIn to Pair(it.isForcedGamePlayUser, it.isAutoScrollEnabled)
                }.collect { (isLoggedIn, gameFlags) ->
                    if (isLoggedIn) {
                        gameFlags.first?.let { isForcedGamePlayUser ->
                            _state.update { it.copy(isStopAndVote = isForcedGamePlayUser) }
                        }
                        gameFlags.second?.let { isAutoScrollEnabled ->
                            _state.update { it.copy(isAutoScrollEnabled = isAutoScrollEnabled) }
                        }
                        listOf(
                            async { getGameRules() },
                            async { getGameIcons() },
                        ).awaitAll()
                    }
                }
        }
    }

    private suspend fun restoreDataFromPrefs() {
        val resultShown = preferences.getBoolean(PrefKeys.IS_RESULT_SHEET_SHOWN.name) ?: false
        val howToPlayShown = preferences.getBoolean(PrefKeys.HOW_TO_PLAY_SHOWN.name) ?: false
        val smileyGameNudgeShown = preferences.getBoolean(PrefKeys.SMILEY_GAME_NUDGE_SHOWN.name) ?: false
        _state.update {
            it.copy(
                isResultSheetShown = resultShown,
                isHowToPlayShown = if (howToPlayShown) it.isHowToPlayShown.map { true } else it.isHowToPlayShown,
                isSmileyGameIntroNudgeShown = smileyGameNudgeShown,
            )
        }
    }

    private suspend fun getGameRules() {
        gameRulesUseCase
            .invoke(Unit)
            .onSuccess { rules ->
                _state.update { currentState ->
                    currentState.copy(
                        gameRules = rules,
                    )
                }
            }.onFailure { }
    }

    private suspend fun getGameIcons() {
        gameIconsUseCase
            .invoke(Unit)
            .onSuccess { config ->
                _state.update { currentState ->
                    currentState.copy(
                        gameIcons = config.availableSmileys,
                        lossPenalty = config.lossPenalty,
                    )
                }
            }.onFailure { }
    }

    fun setClickedIcon(
        icon: GameIcon,
        feedDetails: FeedDetails,
        isTutorialVote: Boolean,
    ) {
        val gameState = _state.value
        if (gameState.isLoading) return
        viewModelScope.launch {
            if (gameState.coinBalance >= gameState.lossPenalty) {
                castVote(icon, feedDetails, isTutorialVote)
            } else {
                refreshBalance()
            }
        }
    }

    private suspend fun refreshBalance() {
        sessionManager.userPrincipal?.let { principal ->
            setLoading(true)
            _state.update { it.copy(refreshBalanceState = RefreshBalanceState.LOADING) }

            autoRechargeBalanceUseCase
                .invoke(parameter = AutoRechargeBalanceRequest(principalId = principal))
                .onSuccess { result ->
                    setLoading(false)
                    _state.update {
                        it.copy(
                            coinBalance = result.coins,
                            refreshBalanceState = RefreshBalanceState.SUCCESS,
                        )
                    }
                    sessionManager.updateCoinBalance(result.coins)
                    gameTelemetry.onAirdropClaimSuccess(result.coins.toInt())
                }.onFailure {
                    setLoading(false)
                    _state.update {
                        it.copy(refreshBalanceState = RefreshBalanceState.FAILURE)
                    }
                    gameTelemetry.onAirdropClaimFailure()
                }
        }
    }

    private suspend fun castVote(
        icon: GameIcon,
        feedDetails: FeedDetails,
        isTutorialVote: Boolean,
    ) {
        _state.update { currentState ->
            // Create initial game result outside state update
            val initialGameResult = Pair(icon, VoteResult(0, "", false))
            val updatedGameResult = currentState.gameResult.toMutableMap()
            updatedGameResult[feedDetails.videoID] = initialGameResult
            currentState.copy(
                gameResult = updatedGameResult,
                lastVotedCount = currentState.lastVotedCount + 1,
            )
        }
        sessionManager.userPrincipal?.let { principal ->
            setLoading(true)
            gameTelemetry.onGameVoted(
                feedDetails = feedDetails,
                lossPenalty = _state.value.lossPenalty,
                optionChosen = icon.imageName.name.lowercase(),
                isTutorialVote = isTutorialVote,
            )
            castVoteUseCase
                .invoke(
                    parameter =
                        CastVoteRequest(
                            principalId = principal,
                            videoId = feedDetails.videoID,
                            gameIconId = icon.id,
                        ),
                ).onSuccess { result ->
                    if (result is CastVoteResponse.Success) {
                        sessionManager.updateDailyRank(result.newPosition)
                        if (result.isBanned) {
                            result.banMessage?.let { banMessage ->
                                ToastManager.showWarning(
                                    type =
                                        ToastType.Small(
                                            message = banMessage,
                                        ),
                                    duration = ToastDuration.INDEFINITE,
                                )
                            }
                        }
                    }
                    setFeedGameResult(
                        videoId = feedDetails.videoID,
                        voteResult = result.toVoteResult(),
                        feedDetails = feedDetails,
                        icon = icon,
                        isTutorialVote = isTutorialVote,
                    )
                }.onFailure { setLoading(false) }
        }
    }

    fun getFeedGameResult(videoId: String): Int =
        _state
            .value
            .gameResult[videoId]
            ?.second
            ?.coinDelta ?: 0

    fun getFeedGameResultError(videoId: String): String =
        _state
            .value
            .gameResult[videoId]
            ?.second
            ?.errorMessage ?: ""

    fun hasShownCoinDeltaAnimation(videoId: String): Boolean =
        _state
            .value
            .gameResult[videoId]
            ?.second
            ?.hasShownAnimation ?: false

    fun markCoinDeltaAnimationShown(videoId: String) {
        val gameResultPair = _state.value.gameResult[videoId] ?: return
        if (gameResultPair.second.coinDelta == 0 && gameResultPair.second.errorMessage.isEmpty()) return
        _state.update { currentState ->
            val updatedGameResult =
                currentState.gameResult.toMutableMap().apply {
                    this[videoId] = this[videoId]?.let {
                        it.copy(second = it.second.copy(hasShownAnimation = true))
                    } ?: gameResultPair
                }
            currentState.copy(gameResult = updatedGameResult)
        }
    }

    private fun setFeedGameResult(
        videoId: String,
        voteResult: VoteResult,
        feedDetails: FeedDetails,
        icon: GameIcon,
        isTutorialVote: Boolean,
    ) {
        viewModelScope.launch {
            // Get current state once to avoid multiple reads
            val currentState = _state.value
            // Calculate all updates outside state update
            val updatedGameResult =
                currentState.gameResult.toMutableMap().apply {
                    get(videoId)?.let { currentPair ->
                        // If this vote result is for a different video than current page, mark as shown
                        val shouldMarkShown = videoId != currentState.currentVideoId
                        put(
                            videoId,
                            currentPair
                                .copy(
                                    second = voteResult.copy(hasShownAnimation = shouldMarkShown),
                                ),
                        )
                    }
                }
            val newCoinBalance = currentState.coinBalance + voteResult.coinDelta
            val shouldShowResultSheet = !currentState.isResultSheetShown && newCoinBalance != currentState.coinBalance
            // Single atomic state update for all game result changes
            _state.update {
                it.copy(
                    gameResult = updatedGameResult,
                    coinBalance = newCoinBalance,
                    animateCoinBalance = newCoinBalance != it.coinBalance,
                    showResultSheet = shouldShowResultSheet,
                    isLoading = false,
                    lastBalanceDifference = voteResult.coinDelta,
                    isSmileyGameIntroNudgeShown = true,
                )
            }
            preferences.putBoolean(PrefKeys.SMILEY_GAME_NUDGE_SHOWN.name, true)
            if (shouldShowResultSheet) {
                preferences.putBoolean(PrefKeys.IS_RESULT_SHEET_SHOWN.name, true)
                _state.update { it.copy(isResultSheetShown = true) }
            }

            // Update session after state update is complete
            sessionManager.updateCoinBalance(newCoinBalance)
            // Minor delay for super properties to be set
            delay(DELAY_FOR_SESSION_PROPERTIES)
            gameTelemetry.onGamePlayed(
                feedDetails = feedDetails,
                lossPenalty = _state.value.lossPenalty,
                optionChosen = icon.imageName.name.lowercase(),
                coinDelta = voteResult.coinDelta,
                isTutorialVote = isTutorialVote,
            )
        }
    }

    fun setAnimateCoinBalance(shouldAnimate: Boolean) {
        _state.update { it.copy(animateCoinBalance = shouldAnimate) }
    }

    private fun setLoading(isLoading: Boolean) {
        _state.update { it.copy(isLoading = isLoading) }
    }

    fun toggleResultSheet(isVisible: Boolean) {
        _state.update { it.copy(showResultSheet = isVisible) }
    }

    fun toggleAboutGame(isVisible: Boolean) {
        _state.update { it.copy(showAboutGame = isVisible) }
    }

    fun setCurrentVideoId(videoId: String) {
        _state.update { it.copy(currentVideoId = videoId) }
    }

    fun onResultSheetButtonClicked(
        coinDelta: Int,
        ctaType: GameConcludedCtaType,
    ) {
        gameTelemetry.gameConcludedBottomSheetClicked(
            lossPenalty = _state.value.lossPenalty,
            coinDelta = coinDelta,
            ctaType = ctaType,
        )
    }

    fun updateGameType(gameType: GameType) {
        _state.update { it.copy(gameType = gameType) }
        // if we require to toggle value on game type change
        // setHowToPlayShown(false)
    }

    fun setHowToPlayShown(
        pageNo: Int,
        currentPage: Int,
    ) {
        if (pageNo > SHOW_HOW_TO_PLAY_MAX_PAGE || pageNo != currentPage) {
            Logger.d("HowToPlay") { "Skipping 'how to play' update for page $pageNo" }
            return
        }
        Logger.d("HowToPlay") { "Setting 'how to play' shown for page $pageNo" }
        _state.update { state ->
            val updatedList =
                state.isHowToPlayShown.toMutableList().apply {
                    if (pageNo in indices) this[pageNo] = true
                }
            state.copy(isHowToPlayShown = updatedList)
        }
        if (_state.value.isHowToPlayShown.all { it }) {
            Logger.d("HowToPlay") { "All 'how to play' pages shown. Marking as shown in preferences." }
            viewModelScope.launch {
                preferences.putBoolean(PrefKeys.HOW_TO_PLAY_SHOWN.name, true)
            }
        }
    }

    fun hideRefreshBalanceAnimation() {
        viewModelScope.launch {
            delay(REFRESH_BALANCE_ANIM_DISMISS_DELAY_MS)
            _state.update { it.copy(refreshBalanceState = RefreshBalanceState.HIDDEN) }
        }
    }

    fun setSmileyGameNudgeShown(feedDetails: FeedDetails) {
        when (_state.value.nudgeType) {
            NudgeType.MANDATORY -> {
                Logger.d("Nudge") { "setSmileyGameManNudgeShown ${feedDetails.videoID}" }
                _state.update { it.copy(nudgeType = null) }
                gameTelemetry.onForcedGamePlayNudgeShown(feedDetails)
            }
            NudgeType.INTRO -> {
                Logger.d("Nudge") { "setSmileyGameIntroNudgeShown ${feedDetails.videoID}" }
                _state.update { it.copy(nudgeType = null) }
                if (!_state.value.isSmileyGameIntroNudgeShown) {
                    _state.update { it.copy(isSmileyGameIntroNudgeShown = true) }
                    viewModelScope.launch {
                        Logger.d("Nudge") { "marking smiley game shown" }
                        preferences.putBoolean(PrefKeys.SMILEY_GAME_NUDGE_SHOWN.name, true)
                        gameTelemetry.onGameTutorialShown(feedDetails)
                    }
                }
            }
            NudgeType.ONBOARDING_START, NudgeType.ONBOARDING_END, NudgeType.ONBOARDING_OTHERS -> {
                // Onboarding nudges are handled by feed module, just clear the nudge type
                _state.update { it.copy(nudgeType = null) }
            }
            null -> Unit
        }
    }

    fun showNudge(
        nudgeIntention: NudgeType,
        pageNo: Int,
        feedDetailsSize: Int,
    ) {
        val currentState = _state.value
        if (currentState.isLoading || currentState.nudgeType != null) return
        when (nudgeIntention) {
            NudgeType.MANDATORY -> {
                if (feedDetailsSize != currentState.lastVotedCount && currentState.isStopAndVote) {
                    Logger.d("Nudge") { "Showing mandatory nudge" }
                    _state.update { it.copy(nudgeType = NudgeType.MANDATORY, isDefaultMandatoryNudgeShown = true) }
                }
            }
            NudgeType.INTRO -> {
                if (!currentState.isSmileyGameIntroNudgeShown && pageNo >= NUDGE_PAGE && !currentState.isStopAndVote) {
                    Logger.d("Nudge") { "Showing intro nudge" }
                    _state.update { it.copy(nudgeType = NudgeType.INTRO) }
                }
            }
            NudgeType.ONBOARDING_START, NudgeType.ONBOARDING_END, NudgeType.ONBOARDING_OTHERS -> {
                // Onboarding nudges are set externally by feed module, not through showNudge
                // This case should not be reached, but included for exhaustiveness
            }
        }
    }

    companion object {
        const val SHOW_HOW_TO_PLAY_MAX_PAGE = 3
        private const val NUDGE_PAGE = 3
        private const val REFRESH_BALANCE_ANIM_DISMISS_DELAY_MS = 2000L
    }
}

data class GameState(
    val lossPenalty: Int = Int.MAX_VALUE,
    val gameIcons: List<GameIcon>,
    val gameResult: Map<String, Pair<GameIcon, VoteResult>> = emptyMap(),
    val coinBalance: Long,
    val animateCoinBalance: Boolean = false,
    val isLoading: Boolean = false,
    val showResultSheet: Boolean = false,
    val showAboutGame: Boolean = false,
    val gameRules: List<AboutGameItem>,
    val currentVideoId: String = "",
    val lastBalanceDifference: Int = 0,
    val gameType: GameType = GameType.SMILEY,
    val isResultSheetShown: Boolean = false,
    val isHowToPlayShown: List<Boolean> = List(SHOW_HOW_TO_PLAY_MAX_PAGE) { false },
    val isSmileyGameIntroNudgeShown: Boolean = false,
    val isDefaultMandatoryNudgeShown: Boolean = false,
    val nudgeType: NudgeType? = null,
    val refreshBalanceState: RefreshBalanceState = RefreshBalanceState.HIDDEN,
    val lastVotedCount: Int = 1,
    val isStopAndVote: Boolean = false,
    val isAutoScrollEnabled: Boolean = false,
)

enum class NudgeType {
    MANDATORY,
    INTRO,
    ONBOARDING_START,
    ONBOARDING_END,
    ONBOARDING_OTHERS,
}

enum class RefreshBalanceState {
    HIDDEN,
    LOADING,
    SUCCESS,
    FAILURE,
}
