package com.yral.shared.rust.service.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.data.models.PostDTO
import com.yral.shared.rust.service.services.ICPLedgerServiceFactory
import com.yral.shared.rust.service.services.IndividualUserServiceFactory
import com.yral.shared.rust.service.services.SnsLedgerServiceFactory
import com.yral.shared.rust.service.services.UserPostServiceFactory
import com.yral.shared.uniffi.generated.Account
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.PostDetailsWithUserInfo
import com.yral.shared.uniffi.generated.Result12
import com.yral.shared.uniffi.generated.UpsPostDetailsForFrontend
import com.yral.shared.uniffi.generated.UpsResult3
import com.yral.shared.uniffi.generated.getPostDetailsWithCreatorInfoV1

internal class IndividualUserDataSourceImpl(
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    private val userPostServiceFactory: UserPostServiceFactory,
    private val snsLedgerServiceFactory: SnsLedgerServiceFactory,
    private val icpLedgerServiceFactory: ICPLedgerServiceFactory,
    private val sessionManager: SessionManager,
) : IndividualUserDataSource {
    override suspend fun fetchFeedDetails(post: PostDTO): PostDetailsForFrontend =
        individualUserServiceFactory
            .service(principal = post.canisterID)
            .getIndividualPostDetailsById(post.postID.toULong())

    override suspend fun fetchSCFeedDetails(post: PostDTO): UpsPostDetailsForFrontend =
        userPostServiceFactory
            .service(principal = post.canisterID)
            .getIndividualPostDetailsById(post.postID)

    override suspend fun fetchFeedDetailsWithCreatorInfo(post: PostDTO): PostDetailsWithUserInfo? {
        val identity = sessionManager.identity ?: throw YralException("No identity found")
        return getPostDetailsWithCreatorInfoV1(
            identityData = identity,
            userCanister = post.canisterID,
            postId = post.postID,
            creatorPrincipal = post.publisherUserId,
            nsfwProbability = post.nsfwProbability?.toFloat() ?: 0.0f,
        )
    }

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
    ): UpsResult3 =
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
