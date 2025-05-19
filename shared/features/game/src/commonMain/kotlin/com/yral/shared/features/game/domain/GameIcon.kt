package com.yral.shared.features.game.domain

data class GameIcon(
    val id: String,
    val imageName: String,
)

enum class GameIconNames {
    HEART,
    LAUGH,
    FIRE,
    SURPRISE,
    ROCKET,
}
