package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2

class UpdateProfileDetailsUseCase
    internal constructor(
        appDispatchers: AppDispatchers,
        failureListener: UseCaseFailureListener,
        private val userInfoRepository: UserInfoRepository,
    ) : SuspendUseCase<UpdateProfileDetailsParams, Unit>(
            appDispatchers.network,
            failureListener,
        ) {
        override suspend fun execute(parameter: UpdateProfileDetailsParams) =
            userInfoRepository.updateProfileDetailsV2(
                principal = parameter.principal,
                details =
                    ProfileUpdateDetailsV2(
                        bio = parameter.bio,
                        websiteUrl = parameter.websiteUrl,
                        profilePictureUrl = parameter.profilePictureUrl,
                    ),
            )
    }

data class UpdateProfileDetailsParams(
    val principal: String,
    val bio: String? = null,
    val websiteUrl: String? = null,
    val profilePictureUrl: String? = null,
)
