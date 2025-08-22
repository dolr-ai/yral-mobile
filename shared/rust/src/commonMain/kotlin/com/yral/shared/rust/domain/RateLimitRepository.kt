package com.yral.shared.rust.domain

import com.yral.shared.uniffi.generated.RateLimitStatusWrapper
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

interface RateLimitRepository {
    suspend fun fetchVideoGenerationStatus(
        canisterID: String,
        requestKey: VideoGenRequestKeyWrapper,
    ): Result2Wrapper
    suspend fun getVideoGenFreeCreditsStatus(
        userPrincipal: String,
        isRegistered: Boolean,
    ): RateLimitStatusWrapper?
}
