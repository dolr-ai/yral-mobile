package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.uniffi.generated.Principal
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetUserProfileDetailsV7UseCaseTest {
    @Test
    fun `failed rust profile fetch is recorded as rust`() =
        runBlocking {
            val failureListener = RecordingFailureListener()
            val useCase =
                GetUserProfileDetailsV7UseCase(
                    appDispatchers = AppDispatchers(),
                    useCaseFailureListener = failureListener,
                    userInfoRepository = FailingUserInfoRepository(),
                )

            useCase(
                GetUserProfileDetailsV7Params(
                    principal = "caller-principal",
                    targetPrincipal = "target-principal",
                ),
            )

            assertEquals("GetUserProfileDetailsV7UseCase", failureListener.tag)
            assertEquals(ExceptionType.RUST.name, failureListener.exceptionType)
            assertIs<IllegalStateException>(failureListener.throwable)
            assertEquals("rust profile fetch failed", failureListener.throwable?.message)
            assertEquals("onFailure", failureListener.message)
        }
}

private class RecordingFailureListener : UseCaseFailureListener {
    var throwable: Throwable? = null
        private set
    var tag: String? = null
        private set
    var message: String? = null
        private set
    var exceptionType: String? = null
        private set

    override fun onFailure(
        throwable: Throwable,
        tag: String?,
        message: () -> String,
        exceptionType: String?,
    ) {
        this.throwable = throwable
        this.tag = tag
        this.message = message()
        this.exceptionType = exceptionType
    }
}

private class FailingUserInfoRepository : UserInfoRepository {
    override suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    ) {
        error("not used")
    }

    override suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    ) {
        error("not used")
    }

    override suspend fun getUserProfileDetailsV7(
        principal: Principal,
        targetPrincipal: Principal,
    ): UserProfileDetails = throw IllegalStateException("rust profile fetch failed")

    override suspend fun getUsersProfileDetails(
        principal: Principal,
        targetPrincipalIds: List<String>,
    ): Map<String, UserProfileDetails> = emptyMap()

    override suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowersPageResult = error("not used")

    override suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowingPageResult = error("not used")

    override suspend fun updateProfileDetailsV2(
        principal: Principal,
        details: ProfileUpdateDetailsV2,
    ) {
        error("not used")
    }

    override suspend fun acceptNewUserRegistrationV2(
        principal: Principal,
        newPrincipal: Principal,
        authenticated: Boolean,
        mainAccount: Principal?,
    ) {
        error("not used")
    }
}
