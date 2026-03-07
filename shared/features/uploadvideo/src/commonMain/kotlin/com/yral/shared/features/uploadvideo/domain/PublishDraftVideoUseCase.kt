package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class PublishDraftVideoUseCase internal constructor(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: UploadRepository,
) : SuspendUseCase<PublishDraftVideoUseCase.Param, Unit>(appDispatchers.network, failureListener) {
    override val exceptionType: String = ExceptionType.UPLOAD_VIDEO.name

    override suspend fun execute(parameter: Param) {
        repository.markPostAsPublished(parameter.postId)
    }

    data class Param(
        val postId: String,
    )
}
