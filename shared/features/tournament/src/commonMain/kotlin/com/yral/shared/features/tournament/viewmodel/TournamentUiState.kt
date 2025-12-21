package com.yral.shared.features.tournament.viewmodel

import com.yral.shared.features.tournament.domain.model.Tournament

data class TournamentUiState(
    val selectedTab: Tab = Tab.All,
    val tournaments: List<Tournament> = emptyList(),
    val prizeBreakdownTournament: Tournament? = null,
    val isLoggedIn: Boolean = false,
) {
    enum class Tab { All, History }
}
