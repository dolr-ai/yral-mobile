package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.GetGameRulesUseCase
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.GameIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(
    appDispatchers: AppDispatchers,
    private val gameIconsUseCase: GetGameIconsUseCase,
    private val gameRulesUseCase: GetGameRulesUseCase,
) : ViewModel() {
    companion object {
        const val GAME_RESULT_API_DELAY = 2000L
        const val WIN_PRIZE = 30
        const val LOSE_PENALTY = -10
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)
    private val _state =
        MutableStateFlow(
            GameState(
                gameIcons = emptyList(),
                gameRules = emptyList(),
                coinBalance = 2000,
            ),
        )
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            getGameRules()
            // First get coin balance
            getGameIcons()
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
            // Temp mocking of api result
            delay(GAME_RESULT_API_DELAY)
            setFeedGameResult(
                videoId = videoId,
                coinDelta =
                    if (Random.nextBoolean()) {
                        WIN_PRIZE
                    } else {
                        LOSE_PENALTY
                    },
            )
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
            _state.emit(
                _state.value.copy(
                    gameResult = temp,
                    coinBalance = _state.value.coinBalance.plus(coinDelta),
                    animateCoinBalance = true,
                    showResultSheet = true,
                    isLoading = false,
                ),
            )
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

    fun setLoading(isLoading: Boolean) {
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
)
