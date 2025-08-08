package com.yral.shared.features.game.data.models

import com.yral.shared.features.game.domain.models.GameConfig
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.features.game.domain.models.GameIconNames
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.model.GameIconDto

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
        imageName = GameIconNames.fromString(imageName.uppercase()),
        imageUrl = imageUrl,
        clickAnimation = clickAnimation,
    )
