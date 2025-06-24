package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.game.domain.CastVoteUseCase
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.GetGameRulesUseCase
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.VoteResult
import com.yral.shared.features.game.domain.models.toVoteResult
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    appDispatchers: AppDispatchers,
    private val preferences: Preferences,
    private val sessionManager: SessionManager,
    private val gameIconsUseCase: GetGameIconsUseCase,
    private val gameRulesUseCase: GetGameRulesUseCase,
    private val castVoteUseCase: CastVoteUseCase,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)
    private val _state =
        MutableStateFlow(
            GameState(
                gameIcons = emptyList(),
                gameRules = emptyList(),
                coinBalance = sessionManager.coinBalance.value,
            ),
        )
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            // Get result sheet shown preference outside state update
            preferences.getBoolean(PrefKeys.IS_RESULT_SHEET_SHOWN.name)?.let { shown ->
                _state.update { currentState ->
                    currentState.copy(
                        isResultSheetShown = shown,
                    )
                }
            }
            listOf(
                async { getGameRules() },
                async { getGameIcons() },
            ).awaitAll()
        }
        viewModelScope.launch {
            // Observe coin balance changes
            sessionManager.coinBalance.collect { balance ->
                Logger.d("xxxx coin balance collected $balance")
                _state.update { currentState ->
                    currentState.copy(
                        coinBalance = balance,
                    )
                }
            }
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
        videoId: String,
    ) {
        coroutineScope.launch {
            _state.update { currentState ->
                // Create initial game result outside state update
                val initialGameResult = Pair(icon, VoteResult(0, "", false))
                val updatedGameResult = currentState.gameResult.toMutableMap()
                updatedGameResult[videoId] = initialGameResult
                currentState.copy(
                    gameResult = updatedGameResult,
                )
            }
            setLoading(true)
            sessionManager.getUserPrincipal()?.let { principal ->
                castVoteUseCase
                    .invoke(
                        parameter =
                            CastVoteRequest(
                                principalId = principal,
                                videoId = videoId,
                                gameIconId = icon.id,
                            ),
                    ).onSuccess { result ->
                        setFeedGameResult(
                            videoId = videoId,
                            voteResult = result.toVoteResult(),
                        )
                    }.onFailure { setLoading(false) }
            }
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
        if (_state.value.gameResult[videoId]
                ?.second
                ?.coinDelta == 0
        ) {
            return
        }
        coroutineScope.launch {
            _state.update { currentState ->
                val updatedGameResult = currentState.gameResult.toMutableMap()
                updatedGameResult[videoId]?.let { currentPair ->
                    updatedGameResult[videoId] =
                        currentPair.copy(
                            second =
                                currentPair.second.copy(
                                    hasShownAnimation = true,
                                ),
                        )
                }
                currentState.copy(
                    gameResult = updatedGameResult,
                )
            }
        }
    }

    private fun setFeedGameResult(
        videoId: String,
        voteResult: VoteResult,
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
                )
            }
            if (shouldShowResultSheet) {
                preferences.putBoolean(PrefKeys.IS_RESULT_SHEET_SHOWN.name, true)
                _state.update { it.copy(isResultSheetShown = true) }
            }

            // Update session after state update is complete
            sessionManager.updateCoinBalance(newCoinBalance)
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
    val isResultSheetShown: Boolean = false,
    val currentVideoId: String = "",
    val lastBalanceDifference: Int = 0,
)
