package com.yral.shared.rust.service.domain.models

sealed class Result2 {
    data class Ok(
        val v1: VideoGenRequestStatus,
    ) : Result2()
    data class Err(
        val v1: String,
    ) : Result2()
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
