package com.yral.shared.features.tournament.domain.model

/**
 * Lightweight tournament status check result.
 */
data class TournamentStatusData(
    val tournamentId: String,
    val status: String,
    val participantCount: Int,
    val timeLeftMs: Long? = null,
)
