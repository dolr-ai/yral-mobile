package com.yral.shared.features.game.domain.models

data class LeaderboardError(
    val code: LeaderboardErrorCodes,
    val message: String,
    val throwable: Throwable? = null,
)

enum class LeaderboardErrorCodes {
    MISSING_ID_TOKEN,
    APPCHECK_INVALID,
    ID_TOKEN_INVALID,
    FIRESTORE_ERROR,
    INTERNAL,
    UNKNOWN,
    MISSING_PID,
}
