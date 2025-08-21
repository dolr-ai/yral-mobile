package com.yral.shared.rust.data

import com.yral.shared.uniffi.generated.RateLimitStatusWrapper
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

interface RateLimitDataSource {
    suspend fun fetchVideoGenerationStatus(
        canisterID: String,
        requestKey: VideoGenRequestKeyWrapper,
    ): Result2Wrapper
    suspend fun getVideoGenFreeCreditsStatus(
        canisterID: String,
        isRegistered: Boolean,
    ): RateLimitStatusWrapper?
}
