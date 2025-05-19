package com.yral.shared.features.game.domain

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.libs.useCase.SuspendUseCase

class GetGameIconsUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
) : SuspendUseCase<Unit, List<GameIcon>>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Unit): List<GameIcon> =
        listOf(
            GameIcon(
                id = "1",
                imageName = GameIconNames.LAUGH.name,
            ),
            GameIcon(
                id = "2",
                imageName = GameIconNames.HEART.name,
            ),
            GameIcon(
                id = "3",
                imageName = GameIconNames.FIRE.name,
            ),
            GameIcon(
                id = "4",
                imageName = GameIconNames.SURPRISE.name,
            ),
            GameIcon(
                id = "5",
                imageName = GameIconNames.ROCKET.name,
            ),
        )
}
