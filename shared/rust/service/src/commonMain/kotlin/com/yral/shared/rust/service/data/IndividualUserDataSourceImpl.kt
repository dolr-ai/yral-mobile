package com.yral.shared.rust.service.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.data.models.PostDTO
import com.yral.shared.rust.service.services.ICPLedgerServiceFactory
import com.yral.shared.rust.service.services.SnsLedgerServiceFactory
import com.yral.shared.rust.service.services.UserPostServiceFactory
import com.yral.shared.uniffi.generated.Account
import com.yral.shared.uniffi.generated.UpsPostDetailsForFrontend
import com.yral.shared.uniffi.generated.UpsResult3

internal class IndividualUserDataSourceImpl(
    private val userPostServiceFactory: UserPostServiceFactory,
    private val snsLedgerServiceFactory: SnsLedgerServiceFactory,
    private val icpLedgerServiceFactory: ICPLedgerServiceFactory,
    private val sessionManager: SessionManager,
) : IndividualUserDataSource {
    override suspend fun fetchSCFeedDetails(post: PostDTO): UpsPostDetailsForFrontend =
        userPostServiceFactory
            .service(principal = post.canisterID)
            .getIndividualPostDetailsById(post.postID)

    override suspend fun getSCPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): UpsResult3 =
        userPostServiceFactory
            .service(principalId)
            .getPostsOfThisUserProfileWithPaginationCursor(principalId, startIndex, pageSize)

    override suspend fun getDraftPostsWithPagination(
        startIndex: ULong,
        pageSize: ULong,
    ): UpsResult3 {
        val principalId =
            sessionManager.userPrincipal
                ?: throw YralException("No user principal found")
        return userPostServiceFactory
            .service(principalId)
            .getDraftPostsOfThisUserProfileWithPagination(startIndex, pageSize)
    }

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
