package com.yral.shared.features.tournament.domain.model

sealed class TournamentParticipationState {
    data class RegistrationRequired(
        val tokensRequired: Int,
    ) : TournamentParticipationState()

    data object Registered : TournamentParticipationState()

    data object JoinNow : TournamentParticipationState()

    data class JoinNowWithTokens(
        val tokensRequired: Int,
    ) : TournamentParticipationState()

    data object JoinNowDisabled : TournamentParticipationState()
}
