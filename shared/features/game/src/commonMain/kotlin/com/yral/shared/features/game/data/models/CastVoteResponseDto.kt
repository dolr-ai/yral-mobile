package com.yral.shared.features.game.data.models

import com.yral.shared.features.game.domain.models.CastVoteError
import com.yral.shared.features.game.domain.models.CastVoteErrorCodes
import com.yral.shared.features.game.domain.models.CastVoteResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CastVoteResponseDto(
    @SerialName("outcome")
    val outcome: String,
    @SerialName("coins")
    val coins: Long,
    @SerialName("coin_delta")
    val coinDelta: Int,
    @SerialName("error")
    val error: CastVoteErrorDto? = null,
)

@Serializable
data class CastVoteErrorDto(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)

fun CastVoteResponseDto.toCastVoteResponse(): CastVoteResponse =
    CastVoteResponse(
        outcome = outcome,
        coins = coins,
        coinDelta = coinDelta,
        error =
            error?.let {
                CastVoteError(
                    code = CastVoteErrorCodes.valueOf(it.code),
                    message = it.message,
                )
            },
    )
