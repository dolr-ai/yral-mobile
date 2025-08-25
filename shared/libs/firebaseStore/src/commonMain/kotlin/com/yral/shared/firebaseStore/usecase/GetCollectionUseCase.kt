package com.yral.shared.firebaseStore.usecase

import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.model.QueryOptions
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlin.reflect.KClass

class GetCollectionUseCase<T : FirestoreDocument>(
    private val repository: FBFirestoreRepositoryApi,
    dispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val documentType: KClass<T>,
) : SuspendUseCase<GetCollectionUseCase.Params, List<T>>(
        coroutineDispatcher = dispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Params): List<T> =
        repository
            .queryCollection(
                collectionPath = parameter.collectionName,
                queryOptions = parameter.query,
                documentType = documentType,
            ).getOrThrow()

    data class Params(
        val collectionName: String,
        val query: QueryOptions = QueryOptions(),
    )
}
