package com.yral.shared.features.profile.data

import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.profile.data.models.DeleteVideoRequestBody
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.http.httpDelete
import com.yral.shared.rust.service.services.IndividualUserServiceFactory
import com.yral.shared.uniffi.generated.GetPostsOfUserProfileError
import com.yral.shared.uniffi.generated.Result12
import com.yral.shared.uniffi.generated.delegatedIdentityWireToJson
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.path
import kotlinx.serialization.json.Json

class ProfileDataSourceImpl(
    private val sessionManager: SessionManager,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val httpClient: HttpClient,
    private val json: Json,
) : ProfileDataSource {
    override suspend fun getProfileVideos(
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult {
        val canisterId =
            sessionManager.canisterID
                ?: throw YralException("No canister principal found")

        val service = individualUserServiceFactory.service(canisterId)

        val result =
            service.getPostsOfThisUserProfileWithPaginationCursor(
                startIndex,
                pageSize,
            )

        return when (result) {
            is Result12.Ok -> {
                val posts = result.v1
                ProfileVideosPageResult(
                    posts = posts,
                    hasNextPage = posts.size == pageSize.toInt(),
                    nextStartIndex = startIndex + pageSize,
                )
            }

            is Result12.Err -> {
                when (result.v1) {
                    GetPostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST -> {
                        ProfileVideosPageResult(
                            posts = emptyList(),
                            hasNextPage = false,
                            nextStartIndex = startIndex,
                        )
                    }

                    GetPostsOfUserProfileError.INVALID_BOUNDS_PASSED -> {
                        throw YralException("Invalid bounds passed for pagination")
                    }

                    GetPostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST -> {
                        throw YralException("Exceeded max number of items allowed in one request")
                    }
                }
            }
        }
    }

    override suspend fun deleteVideo(request: DeleteVideoRequest) {
        val canisterId =
            sessionManager.canisterID
                ?: throw YralException("No canister principal found")
        val identity =
            sessionManager.identity
                ?: throw YralException("No identity found")

        val identityWireJson = delegatedIdentityWireToJson(identity)
        val delegatedIdentityWire =
            json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)

        val deleteRequest =
            DeleteVideoRequestBody(
                canisterId = canisterId,
                postId = request.feedDetails.postID.toULong(),
                videoId = request.feedDetails.videoID,
                delegatedIdentityWire = delegatedIdentityWire,
            )

        httpDelete(httpClient) {
            url {
                host = OFF_CHAIN_BASE_URL
                path(DELETE_VIDEO_ENDPOINT)
            }
            setBody(deleteRequest)
        }
    }

    companion object {
        private const val DELETE_VIDEO_ENDPOINT = "/api/v1/posts"
    }
}
