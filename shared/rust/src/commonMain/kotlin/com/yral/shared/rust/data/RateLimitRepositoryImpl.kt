package com.yral.shared.rust.data

import com.yral.shared.rust.domain.RateLimitRepository
import com.yral.shared.uniffi.generated.RateLimitStatusWrapper
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

class RateLimitRepositoryImpl(
    private val dataSource: RateLimitDataSource,
) : RateLimitRepository {
    override suspend fun fetchVideoGenerationStatus(
        canisterID: String,
        requestKey: VideoGenRequestKeyWrapper,
    ): Result2Wrapper =
        dataSource
            .fetchVideoGenerationStatus(canisterID, requestKey)

    override suspend fun getVideoGenFreeCreditsStatus(
        userPrincipal: String,
        isRegistered: Boolean,
    ): RateLimitStatusWrapper? =
        dataSource
            .getVideoGenFreeCreditsStatus(userPrincipal, isRegistered)
}
