package com.yral.shared.features.uploadvideo.domain.models

internal sealed interface UploadStatus {
    data class InProgress(
        val bytesSent: Long,
        val totalBytes: Long?,
    ) : UploadStatus
    data object Success : UploadStatus
    data class Error(
        val exception: Throwable,
    ) : UploadStatus
}

internal sealed interface UploadState {
    data class InProgress(
        val bytesSent: Long,
        val totalBytes: Long?,
    ) : UploadState
    data object Uploaded : UploadState
}
