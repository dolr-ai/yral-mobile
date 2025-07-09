package com.yral.shared.features.uploadvideo.domain

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.features.uploadvideo.domain.models.UploadState
import com.yral.shared.features.uploadvideo.domain.models.UploadStatus
import com.yral.shared.libs.arch.domain.FlowUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class UploadVideoUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: UploadRepository,
) : FlowUseCase<UploadVideoUseCase.Params, UploadState>(appDispatchers.network, failureListener) {
    override fun execute(parameters: Params): Flow<Result<UploadState, Throwable>> =
        repository
            .uploadVideo(parameters.uploadUrl, parameters.filePath)
            .map { uploadStatus ->
                when (uploadStatus) {
                    is UploadStatus.Error -> Err(uploadStatus.exception)
                    is UploadStatus.InProgress ->
                        Ok(
                            UploadState.InProgress(
                                uploadStatus.bytesSent,
                                uploadStatus.totalBytes,
                            ),
                        )

                    UploadStatus.Success -> Ok(UploadState.Uploaded)
                }
            }

    data class Params(
        val uploadUrl: String,
        val filePath: String,
    )
}
