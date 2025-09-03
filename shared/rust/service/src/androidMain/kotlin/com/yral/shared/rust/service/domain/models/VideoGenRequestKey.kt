package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

data class VideoGenRequestKey(
    val principal: String,
    val counter: ULong,
)

internal fun VideoGenRequestKey.toWrapper(): VideoGenRequestKeyWrapper =
    VideoGenRequestKeyWrapper(
        principal = principal,
        counter = counter,
    )
