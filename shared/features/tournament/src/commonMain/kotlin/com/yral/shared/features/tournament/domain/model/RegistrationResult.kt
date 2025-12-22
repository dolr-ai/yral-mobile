package com.yral.shared.features.tournament.domain.model

data class RegistrationResult(
    val status: String,
    val tournamentId: String,
    val coinsPaid: Int,
    val coinsRemaining: Int,
)
