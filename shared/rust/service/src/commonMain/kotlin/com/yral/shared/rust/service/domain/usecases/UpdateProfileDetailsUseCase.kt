package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetails

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
            userInfoRepository.updateProfileDetails(
                principal = parameter.principal,
                details =
                    ProfileUpdateDetails(
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
