package com.yral.shared.features.uploadvideo.data.remote

import com.yral.shared.core.AppConfigurations
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.uploadvideo.data.remote.models.FileUploadStatus
import com.yral.shared.features.uploadvideo.data.remote.models.GenerateVideoRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.GetUploadUrlRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.GetUploadUrlResponseDTO
import com.yral.shared.features.uploadvideo.data.remote.models.ProvidersResponseDto
import com.yral.shared.features.uploadvideo.data.remote.models.UpdateMetaDataRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.UpdateMetaDataResponseDto
import com.yral.shared.features.uploadvideo.data.remote.models.UploadAiVideoFromUrlRequestDto
import com.yral.shared.features.uploadvideo.data.remote.models.parseGenerateVideoResponse
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.http.UPLOAD_FILE_TIME_OUT
import com.yral.shared.http.handleException
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPost
import com.yral.shared.http.httpPostWithStringResponse
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json

internal class UploadVideoRemoteDataSource(
    private val client: HttpClient,
    private val json: Json,
) {
    suspend fun getUploadUrl(dto: GetUploadUrlRequestDto): GetUploadUrlResponseDTO =
        httpPost(client, json) {
            url {
                host = AppConfigurations.UPLOAD_BASE_URL
                path(GET_UPLOAD_URL_PATH)
            }
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

    fun uploadFile(
        uploadUrl: String,
        filePath: String,
    ) = channelFlow {
        uploadFile(uploadUrl, filePath) { bytesSentTotal, contentLength ->
            send(FileUploadStatus.InProgress(bytesSentTotal, contentLength))
        }
        send(FileUploadStatus.Success)
    }.catch { e ->
        emit(FileUploadStatus.Error(e))
    }

    suspend fun updateMetadata(dto: UpdateMetaDataRequestDto): UpdateMetaDataResponseDto =
        httpPost(client, json) {
            url {
                host = AppConfigurations.UPLOAD_BASE_URL
                path(UPDATE_METADATA_PATH)
            }
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

    private suspend fun uploadFile(
        uploadUrl: String,
        filePath: String,
        progressListener: ProgressListener?,
    ): HttpResponse {
        try {
            val response =
                client.post(uploadUrl) {
                    val path = Path(filePath)
                    val bytes =
                        SystemFileSystem.source(path).buffered().use { source ->
                            source.readByteArray()
                        }

                    contentType(ContentType.Video.MP4)
                    setBody(bytes)
                    timeout {
                        requestTimeoutMillis = UPLOAD_FILE_TIME_OUT
                    }
                    onUpload(progressListener)
                }
            if (!response.status.isSuccess()) {
                val body = runCatching { response.bodyAsText() }.getOrDefault("")
                throw YralException("Upload failed (${response.status.value}): $body")
            }
            return response
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            handleException(e)
        }
    }

    suspend fun generateVideo(dto: GenerateVideoRequestDto): GenerateVideoResult =
        try {
            val response: HttpResponse =
                client.post {
                    url {
                        host = AppConfigurations.OFF_CHAIN_BASE_URL
                        path(GENERATE_WITH_IDENTITY_V2_PATH)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(dto)
                }
            response.parseGenerateVideoResponse(json)
        } catch (e: ClientRequestException) {
            e.response.parseGenerateVideoResponse(json)
        } catch (e: ServerResponseException) {
            e.response.parseGenerateVideoResponse(json)
        } catch (_: Exception) {
            throw YralException("Error generating video")
        }

    suspend fun fetchProviders(): ProvidersResponseDto =
        httpGet(client, json) {
            url {
                host = AppConfigurations.OFF_CHAIN_BASE_URL
                path(GET_PROVIDERS_PATH)
            }
        }

    suspend fun uploadAiVideoFromUrl(dto: UploadAiVideoFromUrlRequestDto): String =
        httpPostWithStringResponse(client) {
            url {
                host = AppConfigurations.ANONYMOUS_IDENTITY_BASE_URL
                path(UPLOAD_AI_VIDEO_FROM_URL_PATH)
            }
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

    @Suppress("UnusedPrivateProperty")
    companion object {
        private const val GET_UPLOAD_URL_PATH = "get_upload_url_v3"
        private const val UPDATE_METADATA_PATH = "update_metadata_v2"
        private const val UPLOAD_AI_VIDEO_FROM_URL_PATH = "/api/upload_ai_video_from_url"
        private const val GET_PROVIDERS_PATH = "/api/v2/videogen/providers"
        private const val GET_ALL_PROVIDERS_PATH = "/api/v2/videogen/providers-all" // Use for internal builds
        private const val GENERATE_WITH_IDENTITY_V2_PATH = "/api/v2/videogen/generate"
    }
}
