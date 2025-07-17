package com.yral.shared.features.uploadvideo.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.uploadvideo.data.remote.UploadVideoRemoteDataSource
import com.yral.shared.features.uploadvideo.data.remote.models.toUpdateMetaDataRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.toUploadEndpoint
import com.yral.shared.features.uploadvideo.data.remote.models.toUploadStatus
import com.yral.shared.features.uploadvideo.domain.UploadRepository
import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import com.yral.shared.uniffi.generated.delegatedIdentityWireToJson
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

internal class UploadRepositoryImpl(
    private val remoteDataSource: UploadVideoRemoteDataSource,
    private val sessionManager: SessionManager,
    private val json: Json,
) : UploadRepository {
    override suspend fun fetchUploadUrl() = remoteDataSource.getUploadUrl().toUploadEndpoint()

    override fun uploadVideo(
        uploadUrl: String,
        filePath: String,
    ) = remoteDataSource.uploadFile(uploadUrl, filePath).map { it.toUploadStatus() }

    override suspend fun updateMetadata(uploadFileRequest: UploadFileRequest) {
        val identity =
            sessionManager.identity
                ?: throw YralException("Session not found while finalising video upload")
        val identityWireJson = delegatedIdentityWireToJson(identity)
        val delegatedIdentityWire =
            json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)
        remoteDataSource.updateMetadata(
            uploadFileRequest.toUpdateMetaDataRequestDto(delegatedIdentityWire),
        )
    }
}
