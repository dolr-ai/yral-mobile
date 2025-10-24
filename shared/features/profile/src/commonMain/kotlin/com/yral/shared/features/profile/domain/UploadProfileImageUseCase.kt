package com.yral.shared.features.profile.domain

import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class UploadProfileImageUseCase(
    private val profileRepository: ProfileRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<UploadProfileImageParams, String>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: UploadProfileImageParams): String =
        profileRepository
            .uploadProfileImage(parameter.imageBase64)
}

data class UploadProfileImageParams(
    val imageBase64: String,
)
