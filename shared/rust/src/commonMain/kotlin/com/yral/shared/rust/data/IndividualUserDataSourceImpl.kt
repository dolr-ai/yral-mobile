package com.yral.shared.rust.data

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.rust.services.UserPostServiceFactory
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.Result12
import com.yral.shared.uniffi.generated.ScPostDetailsForFrontend

class IndividualUserDataSourceImpl(
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val userPostServiceFactory: UserPostServiceFactory,
) : IndividualUserDataSource {
    override suspend fun fetchFeedDetails(post: PostDTO): PostDetailsForFrontend =
        individualUserServiceFactory
            .service(principal = post.canisterID)
            .getIndividualPostDetailsById(post.postID.toULong())

    override suspend fun fetchSCFeedDetails(post: PostDTO): ScPostDetailsForFrontend =
        userPostServiceFactory
            .service(principal = post.canisterID)
            .getIndividualPostDetailsById(post.postID)

    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Result12 =
        individualUserServiceFactory
            .service(principalId)
            .getPostsOfThisUserProfileWithPaginationCursor(startIndex, pageSize)

    override suspend fun getSCPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): List<ScPostDetailsForFrontend> =
        userPostServiceFactory
            .service(principalId)
            .getPostsOfThisUserProfileWithPaginationCursor(principalId, startIndex, pageSize)

    companion object {
        const val CLOUD_FLARE_PREFIX = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
        const val CLOUD_FLARE_SUFFIX = "/manifest/video.m3u8"
        const val CLOUD_FLARE_SUFFIX_MP4 = "/downloads/default.mp4"
        const val THUMBNAIL_SUFFIX = "/thumbnails/thumbnail.jpg"
    }
}
