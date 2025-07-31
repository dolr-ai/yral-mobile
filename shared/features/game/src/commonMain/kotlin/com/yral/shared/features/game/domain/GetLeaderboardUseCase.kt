package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.game.domain.models.LeaderboardItem
import com.yral.shared.features.game.domain.models.toLeaderboardItem
import com.yral.shared.firebaseStore.model.LeaderboardItemDto
import com.yral.shared.firebaseStore.model.QueryOptions
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetLeaderboardUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val getLeaderboard: GetCollectionUseCase<LeaderboardItemDto>,
) : SuspendUseCase<Unit, List<LeaderboardItem>>(appDispatchers.network, useCaseFailureListener) {
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
//                .map { it.copy(coins = setOf<Long>(2000, 3000, 4000, 7000).random()) }
//                .sortedByDescending { it.coins }
        // Calculate ranks: same coins = same rank
        var currentRank = 0
        var previousCoins: Long? = null
        return leaderboard.map { dto ->
            if (previousCoins != null && dto.coins != previousCoins) {
                currentRank++
            }
            previousCoins = dto.coins
            dto.toLeaderboardItem(currentRank)
        }
    }

    companion object {
        private const val MAX_LEADERBOARD_SIZE = 10L
        private const val USERS_COLLECTION = "users"
        private const val ORDER_BY_FIELD = "coins"
    }
}
