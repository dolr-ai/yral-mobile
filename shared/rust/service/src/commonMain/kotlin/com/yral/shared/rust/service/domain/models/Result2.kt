package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.Result2Wrapper
import com.yral.shared.uniffi.generated.VideoGenRequestStatusWrapper

sealed class Result2 {
    data class Ok(
        val v1: VideoGenRequestStatus,
    ) : Result2()
    data class Err(
        val v1: String,
    ) : Result2()
}

fun Result2Wrapper.toResult() =
    when (this) {
        is Result2Wrapper.Ok -> Result2.Ok(v1.toVideoGenRequestStatus())
        is Result2Wrapper.Err -> Result2.Err(v1)
    }

sealed class VideoGenRequestStatus {
    data class Failed(
        val v1: String,
    ) : VideoGenRequestStatus()
    data class Complete(
        val v1: String,
    ) : VideoGenRequestStatus()
    data object Processing : VideoGenRequestStatus()
    data object Pending : VideoGenRequestStatus()
}

fun VideoGenRequestStatusWrapper.toVideoGenRequestStatus() =
    when (this) {
        is VideoGenRequestStatusWrapper.Failed -> VideoGenRequestStatus.Failed(v1)
        is VideoGenRequestStatusWrapper.Complete -> VideoGenRequestStatus.Complete(v1)
        is VideoGenRequestStatusWrapper.Processing -> VideoGenRequestStatus.Processing
        is VideoGenRequestStatusWrapper.Pending -> VideoGenRequestStatus.Pending
    }
