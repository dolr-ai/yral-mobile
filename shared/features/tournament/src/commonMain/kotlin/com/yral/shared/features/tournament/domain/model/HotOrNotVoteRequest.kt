package com.yral.shared.features.tournament.domain.model

data class HotOrNotVoteRequest(
    val tournamentId: String,
    val principalId: String,
    val videoId: String,
    val vote: String,
)
