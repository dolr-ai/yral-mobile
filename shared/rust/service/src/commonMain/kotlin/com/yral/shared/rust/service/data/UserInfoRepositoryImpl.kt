package com.yral.shared.rust.service.data

import co.touchlab.kermit.Logger
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource
import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetails
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.models.toDomain
import com.yral.shared.rust.service.domain.models.toFollowerPageResult
import com.yral.shared.rust.service.domain.models.toFollowingPageResult
import com.yral.shared.rust.service.utils.toPrincipalText
import com.yral.shared.uniffi.generated.Principal

class UserInfoRepositoryImpl(
    private val dataSource: UserInfoDataSource,
    private val followersMetadataDataSource: FollowersMetadataDataSource,
) : UserInfoRepository {
    private val logger = Logger.withTag("UserInfoRepository")

    override suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit = dataSource.followUser(principal, targetPrincipal)

    override suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit = dataSource.unfollowUser(principal, targetPrincipal)

    override suspend fun getProfileDetailsV4(
        principal: Principal,
        targetPrincipal: Principal,
    ): UserProfileDetails =
        dataSource
            .getProfileDetailsV4(principal, targetPrincipal)
            .toDomain()

    override suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowersPageResult {
        val response =
            dataSource.getFollowers(
                principal = principal,
                targetPrincipal = targetPrincipal,
                cursorPrincipal = cursorPrincipal,
                limit = limit,
                withCallerFollows = withCallerFollows,
            )

        val usernames =
            runCatching {
                val principalTexts =
                    response.followers
                        .map { it.principalId.toPrincipalText() }
                        .filter { it.isNotBlank() }
                        .distinct()
                followersMetadataDataSource.fetchUsernames(principalTexts)
            }.onFailure { logger.w(throwable = it) { "Failed to fetch follower usernames" } }
                .getOrElse { emptyMap() }

        return response.toFollowerPageResult(usernames)
    }

    override suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowingPageResult {
        val response =
            dataSource.getFollowing(
                principal = principal,
                targetPrincipal = targetPrincipal,
                cursorPrincipal = cursorPrincipal,
                limit = limit,
                withCallerFollows = withCallerFollows,
            )

        val usernames =
            runCatching {
                val principalTexts =
                    response.following
                        .map { it.principalId.toPrincipalText() }
                        .filter { it.isNotBlank() }
                        .distinct()
                followersMetadataDataSource.fetchUsernames(principalTexts)
            }.onFailure { logger.w(throwable = it) { "Failed to fetch following usernames" } }
                .getOrElse { emptyMap() }

        return response.toFollowingPageResult(usernames)
    }

    override suspend fun updateProfileDetails(
        principal: Principal,
        details: ProfileUpdateDetails,
    ) = dataSource.updateProfileDetails(principal, details)
}
