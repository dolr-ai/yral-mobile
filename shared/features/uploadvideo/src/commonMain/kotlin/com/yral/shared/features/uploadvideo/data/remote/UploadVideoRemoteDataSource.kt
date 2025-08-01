package com.yral.shared.features.uploadvideo.data.remote

import com.yral.shared.core.AppConfigurations
import com.yral.shared.features.uploadvideo.data.remote.models.FileUploadStatus
import com.yral.shared.features.uploadvideo.data.remote.models.GetUploadUrlResponseDTO
import com.yral.shared.features.uploadvideo.data.remote.models.UpdateMetaDataRequestDto
import com.yral.shared.http.UPLOAD_FILE_TIME_OUT
import com.yral.shared.http.handleException
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPostWithStringResponse
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.defaultForFilePath
import io.ktor.http.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json

internal class UploadVideoRemoteDataSource(
    private val client: HttpClient,
    private val json: Json,
) {
    suspend fun getUploadUrl(): GetUploadUrlResponseDTO =
        httpGet(client, json) {
            url {
                host = AppConfigurations.UPLOAD_BASE_URL
                path(GET_UPLOAD_URL_PATH)
            }
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

    suspend fun updateMetadata(dto: UpdateMetaDataRequestDto) {
        httpPostWithStringResponse(client) {
            url {
                host = AppConfigurations.UPLOAD_BASE_URL
                path(UPDATE_METADATA_PATH)
            }
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    private suspend fun uploadFile(
        uploadUrl: String,
        filePath: String,
        progressListener: ProgressListener?,
    ): HttpResponse {
        try {
            return client.submitFormWithBinaryData(
                uploadUrl,
                formData {
                    val path = Path(filePath)
                    append(
                        key = "file",
                        value =
                            InputProvider(
                                size = SystemFileSystem.metadataOrNull(path)?.size,
                            ) { SystemFileSystem.source(path).buffered() },
                        headers =
                            Headers.build {
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.defaultForFilePath(filePath).toString(),
                                )
                                append(HttpHeaders.ContentDisposition, "filename=\"${path.name}\"")
                            },
                    )
                },
            ) {
                timeout {
                    requestTimeoutMillis = UPLOAD_FILE_TIME_OUT
                }
                headers.append(HttpHeaders.AcceptEncoding, "gzip")
                onUpload(progressListener)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            handleException(e)
        }
    }

    companion object {
        private const val GET_UPLOAD_URL_PATH = "get_upload_url"
        private const val UPDATE_METADATA_PATH = "update_metadata"
    }
}
