package com.yral.shared.features.leaderboard.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FirebaseFunctionRequest<T>(
    @SerialName("data")
    val data: T,
)
