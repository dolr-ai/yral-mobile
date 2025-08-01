package com.yral.shared.features.game.data.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceError
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceErrorCodes
import com.yral.shared.features.game.domain.models.UpdatedBalance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AutoRechargeBalanceResponseDto {
    @Serializable
    data class Success(
        @SerialName("coins")
        val coins: Long,
    ) : AutoRechargeBalanceResponseDto()

    @Serializable
    data class Error(
        @SerialName("error")
        val error: AutoRechargeBalanceErrorDto,
    ) : AutoRechargeBalanceResponseDto()
}

@Serializable
data class AutoRechargeBalanceErrorDto(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)

fun AutoRechargeBalanceResponseDto.toAutoRechargeBalanceResponse(): Result<UpdatedBalance, AutoRechargeBalanceError> =
    when (this) {
        is AutoRechargeBalanceResponseDto.Success ->
            Ok(UpdatedBalance(coins = coins))

        is AutoRechargeBalanceResponseDto.Error ->
            Err(
                AutoRechargeBalanceError(
                    code =
                        AutoRechargeBalanceErrorCodes.entries.find { it.name == error.code }
                            ?: AutoRechargeBalanceErrorCodes.UNKNOWN,
                    message = error.message,
                ),
            )
    }
