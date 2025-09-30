package com.yral.shared.rust.service.data

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.rust.service.services.ICPLedgerServiceFactory
import com.yral.shared.rust.service.services.IndividualUserServiceFactory
import com.yral.shared.rust.service.services.SnsLedgerServiceFactory
import com.yral.shared.rust.service.services.UserPostServiceFactory
import com.yral.shared.uniffi.generated.Account
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.Result12
import com.yral.shared.uniffi.generated.ScPostDetailsForFrontend
import com.yral.shared.uniffi.generated.ScResult3

internal class IndividualUserDataSourceImpl(
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val userPostServiceFactory: UserPostServiceFactory,
    private val snsLedgerServiceFactory: SnsLedgerServiceFactory,
    private val icpLedgerServiceFactory: ICPLedgerServiceFactory,
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
        canisterId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Result12 =
        individualUserServiceFactory
            .service(canisterId)
            .getPostsOfThisUserProfileWithPaginationCursor(startIndex, pageSize)

    override suspend fun getSCPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): ScResult3 =
        userPostServiceFactory
            .service(principalId)
            .getPostsOfThisUserProfileWithPaginationCursor(principalId, startIndex, pageSize)

    override suspend fun getUserBitcoinBalance(
        canisterId: String,
        principalId: String,
    ): String =
        icpLedgerServiceFactory
            .service(canisterId)
            .icrc1BalanceOf(Account(owner = principalId, subaccount = null))

    override suspend fun getUserDolrBalance(
        canisterId: String,
        principalId: String,
    ): String =
        snsLedgerServiceFactory
            .service(canisterId)
            .icrc1BalanceOf(Account(owner = principalId, subaccount = null))

    internal companion object {
        internal const val CLOUD_FLARE_PREFIX = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
        internal const val CLOUD_FLARE_SUFFIX = "/manifest/video.m3u8"
        internal const val CLOUD_FLARE_SUFFIX_MP4 = "/downloads/default.mp4"
        internal const val THUMBNAIL_SUFFIX = "/thumbnails/thumbnail.jpg"
    }
}
