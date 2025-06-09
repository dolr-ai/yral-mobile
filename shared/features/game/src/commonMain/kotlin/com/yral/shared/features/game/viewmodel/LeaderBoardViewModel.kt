package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.game.domain.GetCurrentUserInfoUseCase
import com.yral.shared.features.game.domain.GetLeaderboardUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LeaderBoardViewModel(
    appDispatchers: AppDispatchers,
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val getCurrentUserInfoUseCase: GetCurrentUserInfoUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

    init {
        fetchLeaderBoard()
        getCurrentUserInfo()
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

    private fun getCurrentUserInfo() {
        coroutineScope.launch {
            val userPrincipal = sessionManager.getUserPrincipal()
            userPrincipal?.let {
                getCurrentUserInfoUseCase
                    .invoke(
                        parameter =
                            GetCurrentUserInfoUseCase.Params(
                                userPrincipalId = userPrincipal,
                            ),
                    ).onSuccess {
                        println("xxxx current user $it")
                    }.onFailure {
                        println("xxxx current user error $it")
                    }
            }
        }
    }
}
