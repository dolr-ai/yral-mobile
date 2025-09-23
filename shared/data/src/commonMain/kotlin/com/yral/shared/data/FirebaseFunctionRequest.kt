package com.yral.shared.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseFunctionRequest<T>(
    @SerialName("data")
    val data: T,
)
