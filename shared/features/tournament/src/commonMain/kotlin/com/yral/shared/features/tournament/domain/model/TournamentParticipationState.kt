package com.yral.shared.features.tournament.domain.model

sealed class TournamentParticipationState {
    data class RegistrationRequired(
        val tokensRequired: Int,
    ) : TournamentParticipationState()

    data object Registered : TournamentParticipationState()

    data class JoinNow(
        val userDiamonds: Int,
    ) : TournamentParticipationState()

    data class JoinNowWithTokens(
        val tokensRequired: Int,
    ) : TournamentParticipationState()

    /** Pro users join with subscription credits instead of tokens. */
    data class JoinNowWithCredit(
        val creditsRequired: Int,
    ) : TournamentParticipationState()

    data object JoinNowDisabled : TournamentParticipationState()
}
