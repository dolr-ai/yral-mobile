package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.mapBoth
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.features.game.domain.GetGameIconsUseCase
import com.yral.shared.features.game.domain.models.GameIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    appDispatchers: AppDispatchers,
    private val gameIconsUseCase: GetGameIconsUseCase,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(appDispatchers.io)
    private val _state =
        MutableStateFlow(
            GameState(
                gameIcons = emptyList(),
                coinBalance = 2000,
            ),
        )
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            // First get coin balance
            gameIconsUseCase
                .invoke(
                    parameter =
                        GetGameIconsUseCase.GetGameIconsParams(
                            coinBalance = _state.value.coinBalance,
                        ),
                ).mapBoth(
                    success = {
                        _state.emit(
                            _state.value.copy(
                                gameIcons = it,
                            ),
                        )
                    },
                    failure = { },
                )
        }
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
                    // showResultSheet = true,
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
}

data class GameState(
    val gameIcons: List<GameIcon>,
    val gameResult: Map<String, Pair<GameIcon, Int>> = emptyMap(),
    val coinBalance: Long,
    val animateCoinBalance: Boolean = false,
    val isLoading: Boolean = false,
    val showResultSheet: Boolean = false,
)
