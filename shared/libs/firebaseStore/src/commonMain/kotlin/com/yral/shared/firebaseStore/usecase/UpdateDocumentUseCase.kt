package com.yral.shared.firebaseStore.usecase

import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class UpdateDocumentUseCase(
    private val repository: FBFirestoreRepositoryApi,
    dispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<UpdateDocumentUseCase.Params, Unit>(
        coroutineDispatcher = dispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Params): Unit =
        repository
            .updateDocument(
                collectionPath = parameter.collectionName,
                documentId = parameter.documentId,
                fieldAndValue = parameter.fieldAndValue,
            ).getOrThrow()

    data class Params(
        val collectionName: String,
        val documentId: String,
        val fieldAndValue: Pair<String, Any?>,
    )
}
