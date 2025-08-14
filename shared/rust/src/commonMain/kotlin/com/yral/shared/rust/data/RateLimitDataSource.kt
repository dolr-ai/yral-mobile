package com.yral.shared.rust.data

import com.yral.shared.uniffi.generated.PollResult2
import com.yral.shared.uniffi.generated.RateLimitStatus
import com.yral.shared.uniffi.generated.VideoGenRequestKey

interface RateLimitDataSource {
    suspend fun fetchVideoGenerationStatus(requestKey: VideoGenRequestKey): PollResult2
    suspend fun getVideoGenFreeCreditsStatus(
        canisterId: String,
        isRegistered: Boolean,
    ): RateLimitStatus?
}
