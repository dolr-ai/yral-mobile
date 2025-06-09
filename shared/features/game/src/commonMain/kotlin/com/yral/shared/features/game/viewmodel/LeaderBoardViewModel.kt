package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.features.game.domain.GetLeaderboardUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LeaderBoardViewModel(
    appDispatchers: AppDispatchers,
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

    init {
        fetchLeaderBoard()
    }

    private fun fetchLeaderBoard() {
        coroutineScope.launch {
            getLeaderboardUseCase
                .invoke(Unit)
                .onSuccess {
                    println("xxxx leaderboard $it")
                }.onFailure {
                    println("xxxx leaderboard error $it")
                }
        }
    }
}
