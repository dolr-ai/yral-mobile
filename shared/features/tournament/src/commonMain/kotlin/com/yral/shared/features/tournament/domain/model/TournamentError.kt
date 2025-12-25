package com.yral.shared.features.tournament.domain.model

data class TournamentError(
    val code: TournamentErrorCodes,
    val message: String,
    val throwable: Throwable? = null,
)

enum class TournamentErrorCodes {
    MISSING_ID_TOKEN,
    ID_TOKEN_INVALID,
    TOURNAMENT_NOT_FOUND,
    ALREADY_REGISTERED,
    TOURNAMENT_NOT_OPEN,
    INSUFFICIENT_COINS,
    NOT_REGISTERED,
    TOURNAMENT_NOT_LIVE,
    TOURNAMENT_STILL_ACTIVE,
    DUPLICATE_VOTE,
    NO_DIAMONDS,
    SMILEY_NOT_ALLOWED,
    MISSING_TOURNAMENT_ID,
    MISSING_PRINCIPAL_ID,
    MISSING_VIDEO_ID,
    MISSING_SMILEY_ID,
    INVALID_DATE,
    INVALID_STATUS,
    METHOD_NOT_ALLOWED,
    INTERNAL,
    UNKNOWN,
    ;

    companion object {
        fun fromCode(code: String): TournamentErrorCodes = entries.find { it.name == code } ?: UNKNOWN
    }
}
