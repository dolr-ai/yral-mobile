package com.yral.shared.features.tournament.viewmodel

import com.yral.shared.core.session.ProDetails
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.TournamentError

data class TournamentUiState(
    val selectedTab: Tab = Tab.All,
    val tournaments: List<Tournament> = emptyList(),
    val prizeBreakdownTournament: Tournament? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: TournamentError? = null,
    val isRegistering: Boolean = false,
    val proDetails: ProDetails = ProDetails(),
) {
    enum class Tab { All, History }
}
