package com.yral.shared.features.game.data.models

import com.yral.shared.features.game.domain.models.CastVoteError
import com.yral.shared.features.game.domain.models.CastVoteErrorCodes
import com.yral.shared.features.game.domain.models.CastVoteResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CastVoteResponseDto {
    @Serializable
    data class Success(
        @SerialName("outcome")
        val outcome: String,
        @SerialName("coins")
        val coins: Long,
        @SerialName("coin_delta")
        val coinDelta: Int,
        @SerialName("new_position")
        val newPosition: Long? = null,
    ) : CastVoteResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: CastVoteErrorDto,
    ) : CastVoteResponseDto()
}

@Serializable
data class CastVoteErrorDto(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)

fun CastVoteResponseDto.toCastVoteResponse(): CastVoteResponse =
    when (this) {
        is CastVoteResponseDto.Success ->
            CastVoteResponse.Success(
                outcome = outcome,
                coins = coins,
                coinDelta = coinDelta,
                newPosition = newPosition,
            )

        is CastVoteResponseDto.Error ->
            CastVoteResponse.Error(
                error =
                    CastVoteError(
                        code = CastVoteErrorCodes.valueOf(error.code),
                        message = error.message,
                    ),
            )
    }
