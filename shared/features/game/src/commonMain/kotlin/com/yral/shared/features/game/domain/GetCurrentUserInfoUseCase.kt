package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.game.domain.models.CurrentUserInfo
import com.yral.shared.firebaseStore.model.LeaderboardItemDto
import com.yral.shared.firebaseStore.model.QueryOptions
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.uniffi.generated.propicFromPrincipal

class GetCurrentUserInfoUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val getUserDocumentUseCase: GetFBDocumentUseCase<LeaderboardItemDto>,
    private val firestoreRepository: FBFirestoreRepositoryApi,
) : SuspendUseCase<GetCurrentUserInfoUseCase.Params, CurrentUserInfo>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Params): CurrentUserInfo {
        // Fetch the user document
        val userDocument =
            getUserDocumentUseCase
                .invoke(
                    GetFBDocumentUseCase.Params(
                        collectionPath = USERS_COLLECTION,
                        documentId = parameter.userPrincipalId,
                    ),
                ).getOrThrow()

        // Count users with more coins to determine position
        val usersWithMoreCoins =
            firestoreRepository
                .getCollectionCount(
                    collectionPath = USERS_COLLECTION,
                    queryOptions =
                        QueryOptions(
                            filters =
                                listOf(
                                    QueryOptions.Filter.GreaterThan(
                                        field = COINS_FIELD,
                                        value = userDocument.coins,
                                    ),
                                ),
                        ),
                ).getOrThrow()

        // User position is count of users with more coins + 1
        val leaderboardPosition = (usersWithMoreCoins + 1).toInt()

        return CurrentUserInfo(
            userPrincipalId = userDocument.id,
            profileImageUrl = propicFromPrincipal(userDocument.id),
            coins = userDocument.coins,
            leaderboardPosition = leaderboardPosition,
            rank = leaderboardPosition,
        )
    }

    data class Params(
        val userPrincipalId: String,
    )

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val COINS_FIELD = "coins"
    }
}
