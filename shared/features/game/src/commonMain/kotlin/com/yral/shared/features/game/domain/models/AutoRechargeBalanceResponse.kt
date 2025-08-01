package com.yral.shared.features.game.domain.models

data class UpdatedBalance(
    val coins: Long,
)

data class AutoRechargeBalanceError(
    val code: AutoRechargeBalanceErrorCodes,
    val message: String,
    val throwable: Throwable? = null,
)

enum class AutoRechargeBalanceErrorCodes {
    INSUFFICIENT_COINS,
    MISSING_ID_TOKEN,
    APPCHECK_INVALID,
    ID_TOKEN_INVALID,
    FIRESTORE_ERROR,
    INTERNAL,
    UNKNOWN,
}
