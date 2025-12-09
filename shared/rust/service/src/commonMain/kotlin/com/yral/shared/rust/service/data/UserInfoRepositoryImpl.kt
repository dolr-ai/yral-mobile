package com.yral.shared.rust.service.data

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource
import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetails
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.models.toDomain
import com.yral.shared.rust.service.domain.models.toFollowerPageResult
import com.yral.shared.rust.service.domain.models.toFollowingPageResult
import com.yral.shared.rust.service.domain.performance.RustApiPerformanceTracer
import com.yral.shared.rust.service.domain.performance.traceApiCall
import com.yral.shared.uniffi.generated.Principal

class UserInfoRepositoryImpl(
    private val dataSource: UserInfoDataSource,
    private val followersMetadataDataSource: FollowersMetadataDataSource,
    private val performanceTracer: RustApiPerformanceTracer,
) : UserInfoRepository {
    private val logger = Logger.withTag("UserInfoRepository")

    override suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit =
        traceApiCall(performanceTracer, "followUser") {
            dataSource.followUser(principal, targetPrincipal)
        }

    override suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit =
        traceApiCall(performanceTracer, "unfollowUser") {
            dataSource.unfollowUser(principal, targetPrincipal)
        }

    override suspend fun getProfileDetailsV4(
        principal: Principal,
        targetPrincipal: Principal,
    ): UserProfileDetails =
        traceApiCall(performanceTracer, "getProfileDetailsV4") {
            dataSource
                .getProfileDetailsV4(principal, targetPrincipal)
                .toDomain()
        }

    override suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
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
                    val principalTexts =
                        response.followers
                            .map { it.principalId }
                            .filter { it.isNotBlank() }
                            .distinct()
                    followersMetadataDataSource.fetchUsernames(principalTexts)
                }.onFailure { logger.w(throwable = it) { "Failed to fetch follower usernames" } }
                    .getOrElse { emptyMap() }

            response.toFollowerPageResult(usernames)
        }

    override suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
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
                    val principalTexts =
                        response.following
                            .map { it.principalId }
                            .filter { it.isNotBlank() }
                            .distinct()
                    followersMetadataDataSource.fetchUsernames(principalTexts)
                }.onFailure { logger.w(throwable = it) { "Failed to fetch following usernames" } }
                    .getOrElse { emptyMap() }

            response.toFollowingPageResult(usernames)
        }

    override suspend fun updateProfileDetails(
        principal: Principal,
        details: ProfileUpdateDetails,
    ) = traceApiCall(performanceTracer, "updateProfileDetails") {
        dataSource.updateProfileDetails(principal, details)
    }
}
