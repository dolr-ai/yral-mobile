package com.yral.shared.features.tournament.domain.model

/**
 * Types of tournaments supported in the app.
 */
enum class TournamentType {
    /**
     * Smiley game tournament - users vote with emoji icons.
     */
    SMILEY,

    /**
     * Hot or Not tournament - users swipe right (hot) or left (not) to vote.
     * Vote is compared against AI verdict.
     */
    HOT_OR_NOT,

    ;

    companion object {
        fun fromString(value: String?): TournamentType =
            when (value?.lowercase()) {
                "hot_or_not" -> HOT_OR_NOT
                else -> SMILEY
            }
    }
}
