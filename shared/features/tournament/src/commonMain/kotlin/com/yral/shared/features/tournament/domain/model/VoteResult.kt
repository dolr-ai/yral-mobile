package com.yral.shared.features.tournament.domain.model

data class VoteResult(
    val outcome: VoteOutcome,
    val smiley: VotedSmiley,
    val tournamentWins: Int,
    val tournamentLosses: Int,
    val diamonds: Int,
    val position: Int,
    val diamondDelta: Int? = null,
    val activeParticipantCount: Int = 0,
)

enum class VoteOutcome {
    WIN,
    LOSS,
    ;

    companion object {
        fun fromString(value: String): VoteOutcome =
            when (value.uppercase()) {
                "WIN" -> WIN
                else -> LOSS
            }
    }
}

data class VotedSmiley(
    val id: String,
    val imageUrl: String?,
    val isActive: Boolean?,
    val clickAnimation: String?,
    val imageFallback: String?,
)
