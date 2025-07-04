package com.yral.shared.features.uploadvideo.data.remote

import com.yral.shared.core.AppConfigurations
import com.yral.shared.features.uploadvideo.data.remote.models.FileUploadStatus
import com.yral.shared.features.uploadvideo.data.remote.models.GetUploadUrlResponseDTO
import com.yral.shared.features.uploadvideo.data.remote.models.UpdateMetaDataRequestDto
import com.yral.shared.http.handleException
import com.yral.shared.http.httpGet
import com.yral.shared.http.httpPostWithStringResponse
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.setBody
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.defaultForFilePath
import io.ktor.http.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
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
                host = AppConfigurations.OFF_CHAIN_BASE_URL
                path(GET_UPLOAD_URL_PATH)
            }
        }

    fun uploadFile(
        uploadUrl: String,
        filePath: String,
    ) = callbackFlow {
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
                host = AppConfigurations.OFF_CHAIN_BASE_URL
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
    ) {
        try {
            client.submitFormWithBinaryData(
                uploadUrl,
                formData {
                    val path = Path(filePath)
                    append(
                        key = "file",
                        value = SystemFileSystem.source(path).buffered(),
                        headers =
                            Headers.build {
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.defaultForFilePath(filePath).contentType,
                                )
                                append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.File
                                        .withParameter(
                                            ContentDisposition.Parameters.FileName,
                                            path.name,
                                        ).toString(),
                                )
                            },
                    )
                },
            ) {
                onUpload(progressListener)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            handleException(e) // You might still want to handle the exception globally
        }
    }

    companion object {
        private const val GET_UPLOAD_URL_PATH = "get_upload_url"
        private const val UPDATE_METADATA_PATH = "get_upload_url"
    }
}
