package com.yral.shared.features.game.data

import kotlinx.serialization.SerialName

data class GameIconDto(
    val id: String,
    @SerialName("image_name")
    val imageName: String,
    @SerialName("is_active")
    val isActive: Boolean,
)
