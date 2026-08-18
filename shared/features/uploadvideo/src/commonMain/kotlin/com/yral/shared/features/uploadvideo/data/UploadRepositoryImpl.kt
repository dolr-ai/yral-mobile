package com.yral.shared.features.uploadvideo.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.uploadvideo.data.remote.UploadVideoRemoteDataSource
import com.yral.shared.features.uploadvideo.data.remote.models.GetUploadUrlRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.InProgressDraftsRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.MarkPostAsPublishedRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.toDomain
import com.yral.shared.features.uploadvideo.data.remote.models.toDto
import com.yral.shared.features.uploadvideo.data.remote.models.toRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.toUpdateMetaDataRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.toUploadEndpoint
import com.yral.shared.features.uploadvideo.data.remote.models.toUploadStatus
import com.yral.shared.features.uploadvideo.domain.UploadRepository
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.features.uploadvideo.domain.models.InProgressDraft
import com.yral.shared.features.uploadvideo.domain.models.UploadAiVideoFromUrlRequest
import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.flow.map

internal class UploadRepositoryImpl(
    private val remoteDataSource: UploadVideoRemoteDataSource,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
) : UploadRepository {
    override suspend fun fetchUploadUrl(): UploadEndpoint {
        val publisherUserID =
            sessionManager.userPrincipal
                ?: throw YralException("Session not found while finalising video upload")
        return remoteDataSource
            .getUploadUrl(
                dto =
                    GetUploadUrlRequestDto(
                        publisherUserId = publisherUserID,
                    ),
            ).toUploadEndpoint()
    }

    override fun uploadVideo(
        uploadUrl: String,
        filePath: String,
    ) = remoteDataSource.uploadFile(uploadUrl, filePath).map { it.toUploadStatus() }

    override suspend fun updateMetadata(uploadFileRequest: UploadFileRequest) {
        val creatorPrincipal =
            sessionManager.userPrincipal
                ?: throw YralException("Session not found while finalising video upload")
        val idToken = currentIdToken()
        remoteDataSource.updateMetadata(
            uploadFileRequest.toUpdateMetaDataRequestDto(creatorPrincipal),
            idToken,
        )
    }

    override suspend fun fetchProviders() =
        remoteDataSource
            .fetchProviders()
            .toDomain()

    override suspend fun generateVideo(params: GenerateVideoParams): GenerateVideoResult {
        val idToken = currentIdToken()
        val dto = params.toRequestDto()
        return remoteDataSource.generateVideo(dto, idToken)
    }

    override suspend fun getInProgressDrafts(userId: String): List<InProgressDraft> {
        val idToken = currentIdToken()
        return remoteDataSource
            .getInProgressDrafts(
                InProgressDraftsRequestDto(userId = userId),
                idToken,
            ).toDomain()
    }

    override suspend fun uploadAiVideoFromUrl(request: UploadAiVideoFromUrlRequest): String {
        val idToken = currentIdToken()
        return remoteDataSource.uploadAiVideoFromUrl(request.toDto(), idToken)
    }

    override suspend fun markPostAsPublished(postId: String) {
        val idToken = currentIdToken()
        remoteDataSource.markPostAsPublished(
            MarkPostAsPublishedRequestDto(postId = postId),
            idToken,
        )
    }

    private fun currentIdToken(): String =
        preferences.getString(PrefKeys.ID_TOKEN.name)
            ?: throw YralException("ID token not found while calling upload service")
}
