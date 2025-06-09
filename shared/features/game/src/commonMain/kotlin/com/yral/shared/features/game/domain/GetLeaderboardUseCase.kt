package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.domain.models.LeaderboardItem
import com.yral.shared.features.game.domain.models.toLeaderboardItem
import com.yral.shared.firebaseStore.model.LeaderboardItemDto
import com.yral.shared.firebaseStore.model.QueryOptions
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.libs.useCase.SuspendUseCase

class GetLeaderboardUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val getLeaderboard: GetCollectionUseCase<LeaderboardItemDto>,
) : SuspendUseCase<Unit, List<LeaderboardItem>>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Unit): List<LeaderboardItem> {
        val leaderboard =
            getLeaderboard
                .invoke(
                    parameter =
                        GetCollectionUseCase.Params(
                            collectionName = USERS_COLLECTION,
                            query =
                                QueryOptions(
                                    orderBy =
                                        QueryOptions.OrderBy(
                                            field = ORDER_BY_FIELD,
                                            direction = QueryOptions.OrderBy.Direction.DESCENDING,
                                        ),
                                    limit = MAX_LEADERBOARD_SIZE,
                                ),
                        ),
                ).getOrThrow()
        return leaderboard.map { it.toLeaderboardItem() }
    }

    companion object {
        private const val MAX_LEADERBOARD_SIZE = 10L
        private const val USERS_COLLECTION = "users"
        private const val ORDER_BY_FIELD = "coins"
    }
}
