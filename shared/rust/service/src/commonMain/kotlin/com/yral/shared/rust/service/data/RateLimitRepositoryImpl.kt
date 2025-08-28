package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.RateLimitRepository
import com.yral.shared.uniffi.generated.RateLimitStatusWrapper
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

class RateLimitRepositoryImpl(
    private val dataSource: RateLimitDataSource,
) : RateLimitRepository {
    override suspend fun fetchVideoGenerationStatus(requestKey: VideoGenRequestKeyWrapper): Result2Wrapper =
        dataSource
            .fetchVideoGenerationStatus(requestKey)

    override suspend fun getVideoGenFreeCreditsStatus(
        userPrincipal: String,
        isRegistered: Boolean,
    ): RateLimitStatusWrapper? =
        dataSource
            .getVideoGenFreeCreditsStatus(userPrincipal, isRegistered)
}
