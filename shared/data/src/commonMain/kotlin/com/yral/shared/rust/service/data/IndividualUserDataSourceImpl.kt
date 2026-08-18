package com.yral.shared.rust.service.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.data.models.PostDTO
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.http.spacetime.SpacetimeDBRemoteDataSource
import com.yral.shared.rust.service.domain.models.Posts
import com.yral.shared.rust.service.domain.models.toFeedDetails
import com.yral.shared.rust.service.domain.models.toPosts

internal class IndividualUserDataSourceImpl(
    private val sessionManager: SessionManager,
    private val spacetimeDBRemoteDataSource: SpacetimeDBRemoteDataSource,
) : IndividualUserDataSource {
    override suspend fun fetchSCFeedDetails(post: PostDTO): FeedDetails {
        val spacetimePost =
            spacetimeDBRemoteDataSource.getPostById(post.postID)
                ?: throw YralException("Post not found: ${post.postID}")
        return spacetimePost.toFeedDetails(
            postId = post.postID,
            canisterId = post.canisterID,
            nsfwProbability = post.nsfwProbability,
        )
    }

    override suspend fun getSCPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Posts =
        spacetimeDBRemoteDataSource
            .getPostsOfUserByPrincipal(principalId, startIndex, pageSize)
            .toPosts(canisterId = principalId)

    override suspend fun getDraftPostsWithPagination(
        startIndex: ULong,
        pageSize: ULong,
    ): Posts {
        val principalId =
            sessionManager.userPrincipal
                ?: throw YralException("No user principal found")
        return spacetimeDBRemoteDataSource
            .getDraftPostsOfUserByPrincipal(principalId, startIndex, pageSize)
            .toPosts(canisterId = principalId)
    }

    internal companion object {
        private const val MEDIA_CDN_PREFIX =
            "https://cdn-yral-sfw.yral.com"
        private const val THUMBNAIL_SUFFIX = "-thumbnail.png"

        fun thumbnailUrl(
            videoUid: String,
            publisherUserId: String,
        ) = "$MEDIA_CDN_PREFIX/$publisherUserId/$videoUid$THUMBNAIL_SUFFIX"

        fun videoUrl(
            videoUid: String,
            publisherUserId: String,
        ) = "$MEDIA_CDN_PREFIX/$publisherUserId/$videoUid.mp4"
    }
}

enum class PreferredVideoFormat {
    MP4,
    HLS,
}

expect fun getPreferredVideoFormat(): PreferredVideoFormat
