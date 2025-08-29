package com.yral.shared.rust.data

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.Result12

class IndividualUserDataSourceImpl(
    private val individualUserServiceFactory: IndividualUserServiceFactory,
) : IndividualUserDataSource {
    override suspend fun fetchFeedDetails(post: PostDTO): PostDetailsForFrontend =
        individualUserServiceFactory
            .service(principal = post.canisterID)
            .getIndividualPostDetailsById(post.postID.toULong())

    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Result12 =
        individualUserServiceFactory
            .service(principalId)
            .getPostsOfThisUserProfileWithPaginationCursor(startIndex, pageSize)

    companion object {
        const val CLOUD_FLARE_PREFIX = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
        const val CLOUD_FLARE_SUFFIX = "/manifest/video.m3u8"
        const val CLOUD_FLARE_SUFFIX_MP4 = "/downloads/default.mp4"
        const val THUMBNAIL_SUFFIX = "/thumbnails/thumbnail.jpg"
    }
}
