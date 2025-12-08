package com.yral.shared.features.leaderboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.leaderboard.analytics.LeaderBoardTelemetry
import com.yral.shared.features.leaderboard.domain.GetLeaderboardHistoryUseCase
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistory
import com.yral.shared.features.leaderboard.domain.models.LeaderboardHistoryRequest
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel.Companion.TOP_N_THRESHOLD
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4Params
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4UseCase
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeaderboardHistoryViewModel(
    private val getLeaderboardHistoryUseCase: GetLeaderboardHistoryUseCase,
    private val sessionManager: SessionManager,
    private val leaderBoardTelemetry: LeaderBoardTelemetry,
    private val getProfileDetailsV4UseCase: GetProfileDetailsV4UseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(LeaderboardHistoryState())
    val state: StateFlow<LeaderboardHistoryState> = _state.asStateFlow()

    fun fetchHistory(countryCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val principal = sessionManager.userPrincipal
            if (principal == null) {
                _state.update { it.copy(isLoading = false, error = "") }
                return@launch
            }
            getLeaderboardHistoryUseCase
                .invoke(
                    parameter =
                        LeaderboardHistoryRequest(
                            principalId = principal,
                            countryCode = countryCode,
                        ),
                ).onSuccess { history ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            history = history,
                            selectedIndex = 0,
                        )
                    }
                }.onFailure { error ->
                    // No error to be shown on UI setting error as null
                    _state.update { it.copy(isLoading = false, error = null) }
                }
        }
    }

    fun select(index: Int) {
        _state.update { it.copy(selectedIndex = index) }
    }

    fun isCurrentUser(principal: String) = principal == sessionManager.userPrincipal

    fun isCurrentUserInTop(): Boolean {
        val idx = state.value.selectedIndex
        val item = state.value.history.getOrNull(idx) ?: return false
        val position =
            item.userRow?.position
                ?: item.topRows.firstOrNull { isCurrentUser(it.userPrincipalId) }?.position
                ?: Int.MAX_VALUE
        return position <= TOP_N_THRESHOLD
    }

    fun reportLeaderboardDaySelected(visibleRows: Int) {
        with(_state.value) {
            val index = selectedIndex
            val selected = history.getOrNull(index) ?: return
            val currentUser =
                selected.userRow
                    ?: selected.topRows.firstOrNull { item ->
                        item.userPrincipalId == sessionManager.userPrincipal
                    }
            leaderBoardTelemetry.leaderboardDaySelected(
                day = index,
                date = selected.date,
                rank = currentUser?.position ?: Int.MAX_VALUE,
                visibleRows = visibleRows,
            )
        }
    }

    fun onUserClick(item: com.yral.shared.features.leaderboard.domain.models.LeaderboardItem) {
        if (_state.value.isNavigating) return // Prevent multiple clicks

        // Check if it's the current user - create CanisterData from session
        if (isCurrentUser(item.userPrincipalId)) {
            val canisterData =
                CanisterData(
                    canisterId = sessionManager.canisterID ?: getUserInfoServiceCanister(),
                    userPrincipalId = item.userPrincipalId,
                    profilePic = sessionManager.profilePic ?: item.profileImage,
                    username = sessionManager.username,
                    isCreatedFromServiceCanister = sessionManager.isCreatedFromServiceCanister ?: false,
                    isFollowing = false,
                )
            _state.update { it.copy(navigationEvent = canisterData) }
            return
        }

        viewModelScope.launch {
            sessionManager.userPrincipal?.let { principal ->
                _state.update { it.copy(isNavigating = true) }
                getProfileDetailsV4UseCase(
                    parameter =
                        GetProfileDetailsV4Params(
                            principal = principal,
                            targetPrincipal = item.userPrincipalId,
                        ),
                ).onSuccess { profileDetails ->
                    // Merge LeaderboardItem data with fetched profile details
                    val canisterData =
                        CanisterData(
                            canisterId = getUserInfoServiceCanister(),
                            userPrincipalId = item.userPrincipalId,
                            profilePic = profileDetails.profilePictureUrl ?: item.profileImage,
                            username = item.username,
                            isCreatedFromServiceCanister = true,
                            isFollowing = profileDetails.callerFollowsUser ?: false,
                        )
                    _state.update { it.copy(navigationEvent = canisterData, isNavigating = false) }
                }.onFailure { error ->
                    Logger.e("LeaderboardNavigation") { "Error fetching profile details: $error" }
                    _state.update { it.copy(isNavigating = false) }
                }
            }
        }
    }

    fun onNavigationHandled() {
        _state.update { it.copy(navigationEvent = null) }
    }
}

data class LeaderboardHistoryState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val history: LeaderboardHistory = emptyList(),
    val selectedIndex: Int = 0,
    val isNavigating: Boolean = false,
    val navigationEvent: CanisterData? = null,
)
