package com.yral.shared.features.game.data.models

import com.yral.shared.features.game.domain.models.CastHotOrNotVoteError
import com.yral.shared.features.game.domain.models.CastHotOrNotVoteErrorCodes
import com.yral.shared.features.game.domain.models.CastHotOrNotVoteResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CastHotOrNotVoteResponseDto {
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
    ) : CastHotOrNotVoteResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: CastHotOrNotVoteErrorDto,
    ) : CastHotOrNotVoteResponseDto()
}

@Serializable
data class CastHotOrNotVoteErrorDto(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)

fun CastHotOrNotVoteResponseDto.toCastHotOrNotVoteResponse(): CastHotOrNotVoteResponse =
    when (this) {
        is CastHotOrNotVoteResponseDto.Success ->
            CastHotOrNotVoteResponse.Success(
                outcome = outcome,
                coins = coins,
                coinDelta = coinDelta,
                newPosition = newPosition,
            )

        is CastHotOrNotVoteResponseDto.Error ->
            CastHotOrNotVoteResponse.Error(
                error =
                    CastHotOrNotVoteError(
                        code = CastHotOrNotVoteErrorCodes.valueOf(error.code),
                        message = error.message,
                    ),
            )
    }
