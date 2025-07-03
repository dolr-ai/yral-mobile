package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.features.uploadvideo.domain.models.UploadStatus

internal sealed interface FileUploadStatus {
    data class InProgress(val bytesSent: Long, val totalBytes: Long?) : FileUploadStatus
    data object Success : FileUploadStatus
    data class Error(val exception: Throwable) : FileUploadStatus
}

internal fun FileUploadStatus.toUploadStatus() = when (this) {
    is FileUploadStatus.Error -> UploadStatus.Error(exception)
    is FileUploadStatus.InProgress -> UploadStatus.InProgress(bytesSent, totalBytes)
    FileUploadStatus.Success -> UploadStatus.Success
}
