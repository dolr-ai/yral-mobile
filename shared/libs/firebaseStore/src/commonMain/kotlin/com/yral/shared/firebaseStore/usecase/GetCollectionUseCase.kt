package com.yral.shared.firebaseStore.usecase

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.firebaseStore.model.FirestoreDocument
import com.yral.shared.firebaseStore.repository.FBFirestoreRepositoryApi
import com.yral.shared.libs.useCase.SuspendUseCase
import kotlin.reflect.KClass

class GetCollectionUseCase<T : FirestoreDocument>(
    private val repository: FBFirestoreRepositoryApi,
    dispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val documentType: KClass<T>,
) : SuspendUseCase<String, List<T>>(dispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: String): List<T> =
        repository
            .getCollection(parameter, documentType)
            .getOrThrow()
}
