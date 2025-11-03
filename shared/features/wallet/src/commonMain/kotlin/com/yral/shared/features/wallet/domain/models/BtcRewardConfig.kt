package com.yral.shared.features.wallet.domain.models

data class BtcRewardConfig(
    val viewMileStone: Long,
    val rewardAmountInr: Double?,
    val rewardAmountUsd: Double?,
    val minDurationWatched: Float,
)
