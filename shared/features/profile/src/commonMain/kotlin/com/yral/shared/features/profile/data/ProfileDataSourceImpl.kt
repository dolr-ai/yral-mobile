package com.yral.shared.features.profile.data

import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.AppConfigurations.STORAGE_INTERFACE_BASE_URL
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.profile.data.models.DeleteVideoRequestBody
import com.yral.shared.features.profile.data.models.FollowNotificationDto
import com.yral.shared.features.profile.data.models.UploadProfileImageRequestBody
import com.yral.shared.features.profile.data.models.UploadProfileImageResponse
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.http.httpDelete
import com.yral.shared.http.httpPost
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.models.Posts
import com.yral.shared.rust.service.domain.models.PostsOfUserProfileError
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.serialization.json.Json

class ProfileDataSourceImpl(
    private val sessionManager: SessionManager,
    private val individualUserRepository: IndividualUserRepository,
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
) : ProfileDataSource {
    override suspend fun getProfileVideos(
        canisterId: String,
        userPrincipal: String,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult {
        val result =
            individualUserRepository.getPostsOfThisUserProfileWithPaginationCursor(
                canisterId = canisterId,
                principalId = userPrincipal,
                startIndex = startIndex,
                pageSize = pageSize,
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

    override suspend fun getDraftVideos(
        canisterId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): ProfileVideosPageResult {
        val result =
            individualUserRepository.getDraftPostsWithPagination(
                canisterId = canisterId,
                startIndex = startIndex,
                pageSize = pageSize,
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
        val userPrincipal = sessionManager.userPrincipal ?: throw YralException("No user principal found")
        val idToken =
            preferences.getString(PrefKeys.ID_TOKEN.name)
                ?: throw YralException("No ID token found")

        val deleteRequest =
            request.toDeleteVideoRequestBody(
                principal = userPrincipal,
            )

        httpDelete(httpClient) {
            url {
                host = OFF_CHAIN_BASE_URL
                path(DELETE_VIDEO_ENDPOINT)
            }
            headers { append("authorization", "Bearer $idToken") }
            setBody(deleteRequest)
        }
    }

    override suspend fun uploadProfileImage(imageBase64: String): String {
        val idToken =
            preferences.getString(PrefKeys.ID_TOKEN.name)
                ?: throw YralException("No ID token found")

        val response =
            httpPost<UploadProfileImageResponse>(httpClient, json) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = STORAGE_INTERFACE_BASE_URL
                    path(UPLOAD_PROFILE_ENDPOINT)
                }
                headers { append("authorization", "Bearer $idToken") }
                setBody(
                    UploadProfileImageRequestBody(
                        imageData = imageBase64,
                    ),
                )
            }

        return response.profileImageUrl
    }

    override suspend fun followNotification(request: FollowNotificationDto) {
        val idToken =
            preferences.getString(PrefKeys.ID_TOKEN.name)
                ?: throw YralException("No ID token found")
        httpPost<Unit>(httpClient, json) {
            url {
                protocol = URLProtocol.HTTPS
                host = OFF_CHAIN_BASE_URL
                path(FOLLOW_NOTIFICATION)
            }
            headers { append("authorization", "Bearer $idToken") }
            setBody(request)
        }
    }

    companion object {
        private const val DELETE_VIDEO_ENDPOINT = "/api/v2/posts"
        private const val UPLOAD_PROFILE_ENDPOINT = "/api/v1/user/profile-image"
        private const val FOLLOW_NOTIFICATION = "/api/v1/user/follow-notification"
    }
}

internal fun DeleteVideoRequest.toDeleteVideoRequestBody(
    principal: String,
) = DeleteVideoRequestBody(
    principal = principal,
    postId = feedDetails.postID,
    videoId = feedDetails.videoID,
)
