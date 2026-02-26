package com.yral.shared.rust.service.domain

import com.yral.shared.rust.service.domain.models.PropertyRateLimitConfig
import com.yral.shared.rust.service.domain.models.RateLimitStatus
import com.yral.shared.rust.service.domain.models.Result2
import com.yral.shared.rust.service.domain.models.VideoGenRequestKey

interface RateLimitRepository {
    suspend fun fetchVideoGenerationStatus(
        userPrincipal: String,
        requestKey: VideoGenRequestKey,
    ): Result2
    suspend fun getVideoGenFreeCreditsStatus(
        userPrincipal: String,
        isRegistered: Boolean,
    ): RateLimitStatus?
    suspend fun getPropertyRateLimitConfig(
        userPrincipal: String,
        property: String,
    ): PropertyRateLimitConfig?
}
