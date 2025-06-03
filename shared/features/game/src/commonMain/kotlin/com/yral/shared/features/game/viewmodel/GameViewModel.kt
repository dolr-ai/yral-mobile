package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
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
            // Observe coin balance changes
            sessionManager.coinBalance.collect { balance ->
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
            .invoke(
                parameter =
                    GetGameIconsUseCase.GetGameIconsParams(
                        coinBalance = _state.value.coinBalance,
                    ),
            ).onSuccess { icons ->
                _state.update { currentState ->
                    currentState.copy(
                        gameIcons = icons,
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
                val initialGameResult = Pair(icon, VoteResult(0, ""))
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
                        put(videoId, currentPair.copy(second = voteResult))
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
}

data class GameState(
    val gameIcons: List<GameIcon>,
    val gameResult: Map<String, Pair<GameIcon, VoteResult>> = emptyMap(),
    val coinBalance: Long,
    val animateCoinBalance: Boolean = false,
    val isLoading: Boolean = false,
    val showResultSheet: Boolean = false,
    val showAboutGame: Boolean = false,
    val gameRules: List<AboutGameItem>,
    val isResultSheetShown: Boolean = false,
)
