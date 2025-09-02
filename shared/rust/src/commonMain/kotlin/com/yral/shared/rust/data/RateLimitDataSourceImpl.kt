package com.yral.shared.rust.data

import com.yral.shared.rust.services.RateLimitServiceFactory
import com.yral.shared.uniffi.generated.RateLimitStatusWrapper
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

class RateLimitDataSourceImpl(
    private val rateLimitServiceFactory: RateLimitServiceFactory,
) : RateLimitDataSource {
    override suspend fun fetchVideoGenerationStatus(requestKey: VideoGenRequestKeyWrapper): Result2Wrapper =
        rateLimitServiceFactory
            .service(principal = "")
            .pollVideoGenerationStatus(requestKey)

    override suspend fun getVideoGenFreeCreditsStatus(
        userPrincipal: String,
        isRegistered: Boolean,
    ): RateLimitStatusWrapper? =
        rateLimitServiceFactory
            .service(principal = userPrincipal)
            .getRateLimitStatus(VIDEO_GEN_RATE_LIMIT_PROPERTY, isRegistered)

    companion object {
        private const val VIDEO_GEN_RATE_LIMIT_PROPERTY = "VIDEOGEN"
    }
}
