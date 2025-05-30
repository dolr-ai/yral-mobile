package com.yral.shared.firebaseStore.usecase

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.libs.useCase.SuspendUseCase
import kotlin.reflect.KClass

class GetFBDocumentUseCase<T : FirestoreDocument>(
    private val repository: FBFirestoreRepositoryApi,
    dispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val documentType: KClass<T>,
) : SuspendUseCase<GetFBDocumentUseCase.Params, T>(dispatchers.io, crashlyticsManager) {
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
