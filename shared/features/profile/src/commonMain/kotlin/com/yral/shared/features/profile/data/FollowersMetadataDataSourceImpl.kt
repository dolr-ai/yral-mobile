package com.yral.shared.features.profile.data

import com.yral.shared.core.AppConfigurations.METADATA_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.http.httpPost
import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class FollowersMetadataDataSourceImpl(
    private val httpClient: HttpClient,
    private val json: Json,
) : FollowersMetadataDataSource {
    override suspend fun fetchUsernames(principals: List<String>): Map<String, String> {
        if (principals.isEmpty()) return emptyMap()
        val response: BulkMetadataResponseDto =
            httpPost(
                httpClient = httpClient,
                json = json,
            ) {
                url {
                    host = METADATA_BASE_URL
                    path(METADATA_BULK_PATH)
                }
                contentType(ContentType.Application.Json)
                setBody(MetadataBulkRequestDto(users = principals))
            }
        val data =
            response.ok ?: throw YralException(
                response.err?.toString() ?: "FollowersMetadataDataSourceImpl failed",
            )
        return buildMap {
            data.forEach { (principal, metadata) ->
                val username = metadata?.userName?.takeIf { it.isNotBlank() }
                username?.let { username ->
                    put(principal, username)
                }
            }
        }
    }

    @Serializable
    private data class MetadataBulkRequestDto(
        val users: List<String>,
    )

    @Serializable
    private data class BulkMetadataResponseDto(
        @SerialName("Ok") val ok: Map<String, UserMetadataDto?>? = null,
        @SerialName("Err") val err: JsonElement? = null,
    )

    @Serializable
    private data class UserMetadataDto(
        @SerialName("user_name") val userName: String,
    )

    private companion object {
        private const val METADATA_BULK_PATH = "metadata-bulk"
    }
}
