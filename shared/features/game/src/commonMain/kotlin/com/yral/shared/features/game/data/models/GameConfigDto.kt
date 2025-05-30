package com.yral.shared.features.game.data.models

import com.yral.shared.features.game.domain.models.GameConfig
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameConfigDto(
    @SerialName("available_smileys")
    val availableSmileys: List<GameIconDto>,
    @SerialName("loss_penalty")
    val lossPenalty: Int,
)

@Serializable
data class GameIconDto(
    val id: String,
    @SerialName("image_name")
    val imageName: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("click_animation")
    val clickAnimation: String,
    @SerialName("is_active")
    val isActive: Boolean,
)

fun GameConfigDto.toGameConfig(): GameConfig =
    GameConfig(
        lossPenalty = lossPenalty,
        availableSmileys =
            availableSmileys
                .filter { it.isActive }
                .map { it.toGameIcon() },
    )

fun GameIconDto.toGameIcon(): GameIcon =
    GameIcon(
        id = id,
        imageName =
            GameIconNames.valueOf(
                imageName.uppercase(),
            ),
        imageUrl = imageUrl,
        clickAnimation = clickAnimation,
    )
