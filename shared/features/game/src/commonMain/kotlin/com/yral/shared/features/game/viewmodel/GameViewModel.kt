package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.events.GameConcludedCtaType
import com.yral.shared.analytics.events.GameType
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.DELAY_FOR_SESSION_PROPERTIES
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.game.analytics.GameTelemetry
import com.yral.shared.features.game.domain.AutoRechargeBalanceUseCase
import com.yral.shared.features.game.domain.CastVoteUseCase
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.GetGameRulesUseCase
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceRequest
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.VoteResult
import com.yral.shared.features.game.domain.models.toVoteResult
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.domain.models.FeedDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RefreshBalanceState {
    HIDDEN,
    LOADING,
    SUCCESS,
    FAILURE,
}

@Suppress("TooManyFunctions")
class GameViewModel(
    appDispatchers: AppDispatchers,
    private val preferences: Preferences,
    private val sessionManager: SessionManager,
    private val gameIconsUseCase: GetGameIconsUseCase,
    private val gameRulesUseCase: GetGameRulesUseCase,
    private val castVoteUseCase: CastVoteUseCase,
    private val gameTelemetry: GameTelemetry,
    private val autoRechargeBalanceUseCase: AutoRechargeBalanceUseCase,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)
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
        coroutineScope.launch {
            restoreDataFromPrefs()
            listOf(
                async { getGameRules() },
                async { getGameIcons() },
            ).awaitAll()
        }
        viewModelScope.launch {
            // Observe coin balance changes
            sessionManager.observeSessionProperties().collect { properties ->
                Logger.d("xxxx coin balance collected ${properties.coinBalance}")
                properties.coinBalance?.let { balance ->
                    _state.update { it.copy(coinBalance = balance) }
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
                isHowToPlayShown = howToPlayShown,
                isSmileyGameNudgeShown = smileyGameNudgeShown,
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
    ) {
        val gameState = _state.value
        if (gameState.isLoading) return
        coroutineScope.launch {
            if (gameState.coinBalance >= gameState.lossPenalty) {
                castVote(icon, feedDetails)
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
                }.onFailure {
                    setLoading(false)
                    _state.update {
                        it.copy(refreshBalanceState = RefreshBalanceState.FAILURE)
                    }
                }
        }
    }

    private suspend fun castVote(
        icon: GameIcon,
        feedDetails: FeedDetails,
    ) {
        _state.update { currentState ->
            // Create initial game result outside state update
            val initialGameResult = Pair(icon, VoteResult(0, "", false))
            val updatedGameResult = currentState.gameResult.toMutableMap()
            updatedGameResult[feedDetails.videoID] = initialGameResult
            currentState.copy(
                gameResult = updatedGameResult,
            )
        }
        setLoading(true)
        sessionManager.userPrincipal?.let { principal ->
            gameTelemetry.onGameVoted(
                feedDetails = feedDetails,
                lossPenalty = _state.value.lossPenalty,
                optionChosen = icon.imageName.name.lowercase(),
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
                    setFeedGameResult(
                        videoId = feedDetails.videoID,
                        voteResult = result.toVoteResult(),
                        feedDetails = feedDetails,
                        icon = icon,
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

        coroutineScope.launch {
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
    }

    private fun setFeedGameResult(
        videoId: String,
        voteResult: VoteResult,
        feedDetails: FeedDetails,
        icon: GameIcon,
    ) {
        coroutineScope.launch {
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
            val shouldShowResultSheet =
                !currentState.isResultSheetShown && newCoinBalance != currentState.coinBalance
            // Single atomic state update for all game result changes
            _state.update {
                it.copy(
                    gameResult = updatedGameResult,
                    coinBalance = newCoinBalance,
                    animateCoinBalance = newCoinBalance != it.coinBalance,
                    showResultSheet = shouldShowResultSheet,
                    isLoading = false,
                    lastBalanceDifference = voteResult.coinDelta,
                )
            }
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
            )
        }
    }

    fun setAnimateCoinBalance(shouldAnimate: Boolean) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    animateCoinBalance = shouldAnimate,
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    isLoading = isLoading,
                )
            }
        }
    }

    fun toggleResultSheet(isVisible: Boolean) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    showResultSheet = isVisible,
                )
            }
        }
    }

    fun toggleAboutGame(isVisible: Boolean) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    showAboutGame = isVisible,
                )
            }
        }
    }

    fun setCurrentVideoId(videoId: String) {
        coroutineScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    currentVideoId = videoId,
                )
            }
        }
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

    fun setHowToPlayShown() {
        _state.update { it.copy(isHowToPlayShown = true) }
        coroutineScope.launch {
            preferences.putBoolean(PrefKeys.HOW_TO_PLAY_SHOWN.name, true)
        }
    }

    fun setSmileyGameNudgeShown() {
        _state.update { it.copy(isSmileyGameNudgeShown = true) }
        coroutineScope.launch {
            preferences.putBoolean(PrefKeys.SMILEY_GAME_NUDGE_SHOWN.name, true)
        }
    }

    fun hideRefreshBalanceAnimation() {
        coroutineScope.launch {
            delay(REFRESH_BALANCE_ANIM_DISMISS_DELAY_MS)
            _state.update {
                it.copy(refreshBalanceState = RefreshBalanceState.HIDDEN)
            }
        }
    }

    companion object {
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
    val isHowToPlayShown: Boolean = false,
    val isSmileyGameNudgeShown: Boolean = false,
    val refreshBalanceState: RefreshBalanceState = RefreshBalanceState.HIDDEN,
)
