package com.yral.shared.rust.service.data.models

import com.yral.shared.rust.service.domain.models.VideoGenRequestKey
import com.yral.shared.uniffi.generated.VideoGenRequestKeyWrapper

internal fun VideoGenRequestKey.toWrapper(): VideoGenRequestKeyWrapper =
    VideoGenRequestKeyWrapper(
        principal = principal,
        counter = counter,
    )
