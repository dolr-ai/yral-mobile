package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.RateLimitRepository
import com.yral.shared.rust.service.domain.models.RateLimitStatus
import com.yral.shared.rust.service.domain.models.Result2
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey
import com.yral.shared.rust.service.domain.models.toResult
import com.yral.shared.rust.service.domain.models.toStatus
import com.yral.shared.rust.service.domain.models.toWrapper

class RateLimitRepositoryImpl(
    private val dataSource: RateLimitDataSource,
) : RateLimitRepository {
    override suspend fun fetchVideoGenerationStatus(requestKey: VideoGenRequestKey): Result2 =
        dataSource
            .fetchVideoGenerationStatus(requestKey.toWrapper())
            .toResult()

    override suspend fun getVideoGenFreeCreditsStatus(
        userPrincipal: String,
        isRegistered: Boolean,
    ): RateLimitStatus? =
        dataSource
            .getVideoGenFreeCreditsStatus(userPrincipal, isRegistered)
            ?.toStatus()
}
