package com.yral.shared.rust.service.data.models

import com.yral.shared.rust.service.domain.models.Result2
import com.yral.shared.rust.service.domain.models.VideoGenRequestStatus
import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestStatusWrapper

internal fun Result2Wrapper.toResult() =
    when (this) {
        is Result2Wrapper.Ok -> Result2.Ok(v1.toVideoGenRequestStatus())
        is Result2Wrapper.Err -> Result2.Err(v1)
    }

internal fun VideoGenRequestStatusWrapper.toVideoGenRequestStatus() =
    when (this) {
        is VideoGenRequestStatusWrapper.Failed -> VideoGenRequestStatus.Failed(v1)
        is VideoGenRequestStatusWrapper.Complete -> VideoGenRequestStatus.Complete(v1)
        is VideoGenRequestStatusWrapper.Processing -> VideoGenRequestStatus.Processing
        is VideoGenRequestStatusWrapper.Pending -> VideoGenRequestStatus.Pending
    }
