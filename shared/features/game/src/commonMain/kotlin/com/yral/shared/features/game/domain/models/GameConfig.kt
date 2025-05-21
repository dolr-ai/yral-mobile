package com.yral.shared.features.game.domain.models

data class GameConfig(
    val availableSmileys: List<GameIcon>,
    val lossPenalty: Int,
)

data class GameIcon(
    val id: String,
    val imageName: GameIconNames,
    val imageUrl: String,
    val clickAnimation: String,
)

enum class GameIconNames {
    HEART,
    LAUGH,
    FIRE,
    SURPRISE,
    ROCKET,
}
