package com.yral.shared.firebaseStore.usecase

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.libs.useCase.SuspendUseCase

class UpdateDocumentUseCase(
    private val repository: FBFirestoreRepositoryApi,
    dispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
) : SuspendUseCase<UpdateDocumentUseCase.Params, Unit>(dispatchers.io, crashlyticsManager) {
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
