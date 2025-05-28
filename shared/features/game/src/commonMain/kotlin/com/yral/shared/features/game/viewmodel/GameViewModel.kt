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
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            preferences.getBoolean(PrefKeys.IS_RESULT_SHEET_SHOWN.name)?.let { shown ->
                _state.emit(
                    _state.value.copy(
                        isResultSheetShown = shown,
                    ),
                )
            }
            listOf(
                async { getGameRules() },
                async { getGameIcons() },
            ).awaitAll()
            // Observe coin balance changes
            sessionManager.coinBalance.collect { balance ->
                _state.emit(
                    _state.value.copy(
                        coinBalance = balance,
                    ),
                )
            }
        }
    }

    private suspend fun getGameRules() {
        gameRulesUseCase
            .invoke(Unit)
            .onSuccess {
                _state.emit(
                    _state.value.copy(
                        gameRules = it,
                    ),
                )
            }.onFailure { }
    }

    private suspend fun getGameIcons() {
        gameIconsUseCase
            .invoke(
                parameter =
                    GetGameIconsUseCase.GetGameIconsParams(
                        coinBalance = _state.value.coinBalance,
                    ),
            ).onSuccess {
                _state.emit(
                    _state.value.copy(
                        gameIcons = it,
                    ),
                )
            }.onFailure { }
    }

    fun setClickedIcon(
        icon: GameIcon,
        videoId: String,
    ) {
        coroutineScope.launch {
            val temp = _state.value.gameResult.toMutableMap()
            temp[videoId] = Pair(icon, 0)
            _state.emit(
                _state.value.copy(
                    gameResult = temp,
                ),
            )
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
                            coinDelta = result.coinDelta,
                        )
                    }.onFailure { setLoading(false) }
            }
        }
    }

    fun getFeedGameResult(videoId: String): Int =
        _state
            .value
            .gameResult[videoId]
            ?.second ?: 0

    fun setFeedGameResult(
        videoId: String,
        coinDelta: Int,
    ) {
        coroutineScope.launch {
            val temp = _state.value.gameResult.toMutableMap()
            val tempPair = _state.value.gameResult[videoId]
            tempPair?.let {
                temp[videoId] = tempPair.copy(second = coinDelta)
            }
            val newCoinBalance = _state.value.coinBalance.plus(coinDelta)
            val showResultSheet = !_state.value.isResultSheetShown
            if (showResultSheet) {
                preferences.putBoolean(PrefKeys.IS_RESULT_SHEET_SHOWN.name, true)
                _state.emit(
                    _state.value.copy(isResultSheetShown = true),
                )
            }
            _state.emit(
                _state.value.copy(
                    gameResult = temp,
                    coinBalance = newCoinBalance,
                    animateCoinBalance = true,
                    showResultSheet = showResultSheet,
                    isLoading = false,
                ),
            )
            // Update session with new coin balance
            sessionManager.updateCoinBalance(newCoinBalance)
        }
    }

    fun setAnimateCoinBalance(shouldAnimate: Boolean) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    animateCoinBalance = shouldAnimate,
                ),
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    isLoading = isLoading,
                ),
            )
        }
    }

    fun toggleResultSheet(isVisible: Boolean) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    showResultSheet = isVisible,
                ),
            )
        }
    }

    fun toggleAboutGame(isVisible: Boolean) {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    showAboutGame = isVisible,
                ),
            )
        }
    }

    fun updateCacheFetched() {
        coroutineScope.launch {
            _state.emit(
                _state.value.copy(
                    cacheFetched = true,
                ),
            )
        }
    }
}

data class GameState(
    val gameIcons: List<GameIcon>,
    val gameResult: Map<String, Pair<GameIcon, Int>> = emptyMap(),
    val coinBalance: Long,
    val animateCoinBalance: Boolean = false,
    val isLoading: Boolean = false,
    val showResultSheet: Boolean = false,
    val showAboutGame: Boolean = false,
    val gameRules: List<AboutGameItem>,
    val cacheFetched: Boolean = false,
    val isResultSheetShown: Boolean = false,
)
