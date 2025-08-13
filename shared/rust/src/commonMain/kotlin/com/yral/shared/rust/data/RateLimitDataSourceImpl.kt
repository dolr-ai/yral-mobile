package com.yral.shared.rust.data

import com.yral.shared.rust.services.RateLimitServiceFactory
import com.yral.shared.uniffi.generated.PollResult2
import com.yral.shared.uniffi.generated.RateLimitStatus
import com.yral.shared.uniffi.generated.VideoGenRequestKey

class RateLimitDataSourceImpl(
    private val rateLimitServiceFactory: RateLimitServiceFactory,
) : RateLimitDataSource {
    override suspend fun fetchVideoGenerationStatus(requestKey: VideoGenRequestKey): PollResult2 =
        rateLimitServiceFactory
            .service(principal = RATE_LIMIT_CANISTER)
            .pollVideoGenerationStatus(requestKey)

    override suspend fun getVideoGenFreeCreditsStatus(
        canisterId: String,
        isRegistered: Boolean,
    ): RateLimitStatus? =
        rateLimitServiceFactory
            .service(principal = RATE_LIMIT_CANISTER)
            .getRateLimitStatus(canisterId, VIDEO_GEN_RATE_LIMIT_PROPERTY, isRegistered)

    companion object {
        private const val VIDEO_GEN_RATE_LIMIT_PROPERTY = "VIDEOGEN"

        // hardcoded for now, we can bring this from firebase config
        private const val RATE_LIMIT_CANISTER = "h2jgv-ayaaa-aaaas-qbh4a-cai"
    }
}
