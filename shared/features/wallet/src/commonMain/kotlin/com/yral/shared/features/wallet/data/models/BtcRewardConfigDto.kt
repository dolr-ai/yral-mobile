package com.yral.shared.features.wallet.data.models

import com.yral.shared.features.wallet.domain.models.BtcRewardConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BtcRewardConfigResponseDto(
    @SerialName("config")
    val config: BtcRewardConfigDto? = null,
)

@Serializable
data class BtcRewardConfigDto(
    @SerialName("view_milestone")
    val viewMileStone: Long,
    @SerialName("reward_amount_inr")
    val rewardAmountInr: Double?,
    @SerialName("reward_amount_usd")
    val rewardAmountUsd: Double?,
    @SerialName("min_watch_duration")
    val minDurationWatched: Float,
)

fun BtcRewardConfigDto.toDomain(): BtcRewardConfig =
    BtcRewardConfig(
        viewMileStone = viewMileStone,
        rewardAmountInr = rewardAmountInr,
        rewardAmountUsd = rewardAmountUsd,
        minDurationWatched = minDurationWatched,
    )
