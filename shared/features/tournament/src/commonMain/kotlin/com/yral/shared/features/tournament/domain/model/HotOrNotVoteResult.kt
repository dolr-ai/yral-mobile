package com.yral.shared.features.tournament.domain.model

data class HotOrNotVoteResult(
    val outcome: String,
    val vote: String,
    val aiVerdict: String,
    val diamonds: Int,
    val diamondDelta: Int,
    val wins: Int = 0,
    val losses: Int = 0,
    val position: Int = 0,
)
