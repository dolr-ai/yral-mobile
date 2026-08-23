package com.yral.shared.rust.service.data

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource
import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.models.toFollowerPageResult
import com.yral.shared.rust.service.domain.models.toFollowingPageResult
import com.yral.shared.rust.service.domain.performance.RustApiPerformanceTracer
import com.yral.shared.rust.service.domain.performance.traceApiCall

class UserInfoRepositoryImpl(
    private val dataSource: UserInfoDataSource,
    private val followersMetadataDataSource: FollowersMetadataDataSource,
    private val performanceTracer: RustApiPerformanceTracer,
) : UserInfoRepository {
    private val logger = Logger.withTag("UserInfoRepository")

    override suspend fun followUser(
        principal: String,
        targetPrincipal: String,
    ): Unit =
        traceApiCall(performanceTracer, "followUser") {
            dataSource.followUser(principal, targetPrincipal)
        }

    override suspend fun unfollowUser(
        principal: String,
        targetPrincipal: String,
    ): Unit =
        traceApiCall(performanceTracer, "unfollowUser") {
            dataSource.unfollowUser(principal, targetPrincipal)
        }

    override suspend fun getUserProfileDetailsV7(
        principal: String,
        targetPrincipal: String,
    ): UserProfileDetails =
        traceApiCall(performanceTracer, "getUserProfileDetailsV7") {
            dataSource.getUserProfileDetailsV7(principal, targetPrincipal)
        }

    override suspend fun getUsersProfileDetails(
        principal: String,
        targetPrincipalIds: List<String>,
    ): Map<String, UserProfileDetails> =
        traceApiCall(performanceTracer, "getUserProfileDetailsV7Bulk") {
            dataSource
                .getUsersProfileDetails(principal, targetPrincipalIds)
                .associateBy { it.principalId }
        }

    override suspend fun getFollowers(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowersPageResult =
        traceApiCall(performanceTracer, "getFollowers") {
            val response =
                dataSource.getFollowers(
                    principal = principal,
                    targetPrincipal = targetPrincipal,
                    cursorPrincipal = cursorPrincipal,
                    limit = limit,
                    withCallerFollows = withCallerFollows,
                )

            val usernames =
                runSuspendCatching {
                    val oauthSubjects =
                        response.followers
                            .map { it.oauthSubject }
                            .filter { it.isNotBlank() }
                            .distinct()
                    followersMetadataDataSource.fetchUsernames(oauthSubjects)
                }.onFailure { logger.w(throwable = it) { "Failed to fetch follower usernames" } }
                    .getOrElse { emptyMap() }

            response.toFollowerPageResult(usernames)
        }

    override suspend fun getFollowing(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowingPageResult =
        traceApiCall(performanceTracer, "getFollowing") {
            val response =
                dataSource.getFollowing(
                    principal = principal,
                    targetPrincipal = targetPrincipal,
                    cursorPrincipal = cursorPrincipal,
                    limit = limit,
                    withCallerFollows = withCallerFollows,
                )

            val usernames =
                runSuspendCatching {
                    val oauthSubjects =
                        response.following
                            .map { it.oauthSubject }
                            .filter { it.isNotBlank() }
                            .distinct()
                    followersMetadataDataSource.fetchUsernames(oauthSubjects)
                }.onFailure { logger.w(throwable = it) { "Failed to fetch following usernames" } }
                    .getOrElse { emptyMap() }

            response.toFollowingPageResult(usernames)
        }

    override suspend fun updateProfileDetailsV2(
        principal: String,
        details: ProfileUpdateDetailsV2,
    ) = traceApiCall(performanceTracer, "updateProfileDetailsV2") {
        dataSource.updateProfileDetailsV2(principal, details)
    }

    override suspend fun acceptNewUserRegistrationV2(
        principal: String,
        newPrincipal: String,
        authenticated: Boolean,
        mainAccount: String?,
    ) = traceApiCall(performanceTracer, "acceptNewUserRegistrationV2") {
        dataSource.acceptNewUserRegistrationV2(
            principal = principal,
            newPrincipal = newPrincipal,
            authenticated = authenticated,
            mainAccount = mainAccount,
        )
    }
}
