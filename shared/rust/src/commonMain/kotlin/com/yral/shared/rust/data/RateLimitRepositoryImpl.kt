package com.yral.shared.rust.data

import com.yral.shared.rust.domain.RateLimitRepository
import com.yral.shared.uniffi.generated.PollResult2
import com.yral.shared.uniffi.generated.RateLimitStatus
import com.yral.shared.uniffi.generated.VideoGenRequestKey

class RateLimitRepositoryImpl(
    private val dataSource: RateLimitDataSource,
) : RateLimitRepository {
    override suspend fun fetchVideoGenerationStatus(requestKey: VideoGenRequestKey): PollResult2 =
        dataSource
            .fetchVideoGenerationStatus(requestKey)

    override suspend fun getVideoGenFreeCreditsStatus(
        canisterId: String,
        isRegistered: Boolean,
    ): RateLimitStatus? =
        dataSource
            .getVideoGenFreeCreditsStatus(canisterId, isRegistered)
}
