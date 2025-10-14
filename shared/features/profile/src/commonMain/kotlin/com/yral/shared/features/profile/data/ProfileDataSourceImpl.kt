package com.yral.shared.features.profile.data

import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.profile.data.models.DeleteVideoRequestBody
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.http.httpDelete
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.models.Posts
import com.yral.shared.rust.service.domain.models.PostsOfUserProfileError
import com.yral.shared.rust.service.utils.delegatedIdentityWireToJson
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.path
import kotlinx.serialization.json.Json

class ProfileDataSourceImpl(
    private val sessionManager: SessionManager,
    private val individualUserRepository: IndividualUserRepository,
    private val httpClient: HttpClient,
    private val json: Json,
) : ProfileDataSource {
    override suspend fun getProfileVideos(
        canisterId: String,
        userPrincipal: String,
        isFromServiceCanister: Boolean,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult {
        val result =
            individualUserRepository.getPostsOfThisUserProfileWithPaginationCursor(
                canisterId = canisterId,
                principalId = userPrincipal,
                startIndex = startIndex,
                pageSize = pageSize,
                shouldFetchFromServiceCanisters = isFromServiceCanister,
            )
        return when (result) {
            is Posts.Ok -> {
                val posts = result.v1
                ProfileVideosPageResult(
                    posts = posts.filterNotNull(),
                    hasNextPage = posts.size == pageSize.toInt(),
                    nextStartIndex = startIndex + pageSize,
                )
            }

            is Posts.Err -> {
                when (result.v1) {
                    PostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST -> {
                        ProfileVideosPageResult(
                            posts = emptyList(),
                            hasNextPage = false,
                            nextStartIndex = startIndex,
                        )
                    }

                    PostsOfUserProfileError.INVALID_BOUNDS_PASSED -> {
                        throw YralException("Invalid bounds passed for pagination")
                    }

                    PostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST -> {
                        throw YralException("Exceeded max number of items allowed in one request")
                    }
                }
            }
        }
    }

    override suspend fun deleteVideo(request: DeleteVideoRequest) {
        val userPrincipal =
            sessionManager.userPrincipal
                ?: throw YralException("No user principal found")
        val identity =
            sessionManager.identity
                ?: throw YralException("No identity found")

        val identityWireJson = delegatedIdentityWireToJson(identity)
        val delegatedIdentityWire =
            json.decodeFromString<KotlinDelegatedIdentityWire>(identityWireJson)

        val deleteRequest =
            DeleteVideoRequestBody(
                principal = userPrincipal,
                postId = request.feedDetails.postID,
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
        private const val DELETE_VIDEO_ENDPOINT = "/api/v2/posts"
    }
}
