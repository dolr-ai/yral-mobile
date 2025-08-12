package com.yral.shared.firebaseStore.usecase

import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlin.reflect.KClass

class GetFBDocumentUseCase<T : FirestoreDocument>(
    private val repository: FBFirestoreRepositoryApi,
    dispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val documentType: KClass<T>,
) : SuspendUseCase<GetFBDocumentUseCase.Params, T>(
        coroutineDispatcher = dispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Params): T =
        repository
            .getDocument(
                collectionPath = parameter.collectionPath,
                documentId = parameter.documentId,
                documentType = documentType,
            ).getOrThrow()

    data class Params(
        val collectionPath: String,
        val documentId: String,
    )
}
