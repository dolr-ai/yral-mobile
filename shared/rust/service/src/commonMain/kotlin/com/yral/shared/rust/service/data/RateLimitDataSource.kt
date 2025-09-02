package com.yral.shared.rust.service.data

import com.yral.shared.uniffi.generated.RateLimitStatusWrapper
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

interface RateLimitDataSource {
    suspend fun fetchVideoGenerationStatus(
        userPrincipal: String,
        requestKey: VideoGenRequestKeyWrapper,
    ): Result2Wrapper
    suspend fun getVideoGenFreeCreditsStatus(
        userPrincipal: String,
        isRegistered: Boolean,
    ): RateLimitStatusWrapper?
}
