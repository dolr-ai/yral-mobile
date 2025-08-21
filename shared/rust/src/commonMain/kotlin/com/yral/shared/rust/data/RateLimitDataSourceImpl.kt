package com.yral.shared.rust.data

import com.yral.shared.rust.services.RateLimitServiceFactory
import com.yral.shared.uniffi.generated.RateLimitStatusWrapper
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

class RateLimitDataSourceImpl(
    private val rateLimitServiceFactory: RateLimitServiceFactory,
) : RateLimitDataSource {
    override suspend fun fetchVideoGenerationStatus(
        canisterID: String,
        requestKey: VideoGenRequestKeyWrapper,
    ): Result2Wrapper =
        rateLimitServiceFactory
            .service(principal = canisterID)
            .pollVideoGenerationStatus(requestKey)

    override suspend fun getVideoGenFreeCreditsStatus(
        canisterID: String,
        isRegistered: Boolean,
    ): RateLimitStatusWrapper? =
        rateLimitServiceFactory
            .service(principal = canisterID)
            .getRateLimitStatus(VIDEO_GEN_RATE_LIMIT_PROPERTY, isRegistered)

    companion object {
        private const val VIDEO_GEN_RATE_LIMIT_PROPERTY = "VIDEOGEN"
    }
}
