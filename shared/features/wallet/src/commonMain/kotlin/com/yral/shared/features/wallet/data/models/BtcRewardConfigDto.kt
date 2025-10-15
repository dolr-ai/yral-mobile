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
)

fun BtcRewardConfigDto.toDomain(): BtcRewardConfig =
    BtcRewardConfig(
        viewMileStone = viewMileStone,
    )
