package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.features.uploadvideo.domain.models.UploadAiVideoFromUrlRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadStatus
import kotlinx.coroutines.flow.Flow

internal interface UploadRepository {
    suspend fun fetchUploadUrl(): UploadEndpoint
    fun uploadVideo(
        uploadUrl: String,
        filePath: String,
    ): Flow<UploadStatus>
    suspend fun updateMetadata(uploadFileRequest: UploadFileRequest)
    suspend fun fetchProviders(): List<Provider>
    suspend fun generateVideo(params: GenerateVideoParams): GenerateVideoResult
    suspend fun uploadAiVideoFromUrl(request: UploadAiVideoFromUrlRequest)
}
