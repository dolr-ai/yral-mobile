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
    val unicode: String,
)

enum class GameIconNames {
    HEART,
    LAUGH,
    FIRE,
    SURPRISE,
    ROCKET,
    PUKE,
    UNKNOWN, ;

    companion object {
        fun fromString(value: String?): GameIconNames = entries.find { it.name == value } ?: UNKNOWN
    }
}
