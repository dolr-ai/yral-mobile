@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.yral.shared.data.domain.useCases

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.DailyStreak
import com.yral.shared.data.domain.models.VideoViews
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.testsupport.commonapis.FakeCommonApis
import com.yral.shared.testsupport.preferences.FakePreferences
import com.yral.shared.testsupport.usecase.NoOpUseCaseFailureListener
import com.yral.shared.testsupport.usecase.RecordingUseCaseFailureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FetchDailyStreakUseCaseTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var preferences: FakePreferences

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferences = FakePreferences()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildUseCase(
        commonApis: CommonApis,
        useCaseFailureListener: UseCaseFailureListener = NoOpUseCaseFailureListener(),
    ) = FetchDailyStreakUseCase(
        commonApis = commonApis,
        preferences = preferences,
        appDispatchers = AppDispatchers(),
        useCaseFailureListener = useCaseFailureListener,
    )

    @Test
    fun returnsStreakFromCommonApis() =
        runTest {
            preferences.putString(PrefKeys.ID_TOKEN.name, "test-token")
            val expected = buildDailyStreak(streakCount = 7L)
            val result =
                buildUseCase(FakeCommonApis(dailyStreak = expected))
                    .invoke(FetchDailyStreakUseCase.Params(userPrincipal = "principal-1"))
            assertEquals(Ok(expected), result)
        }

    @Test
    fun propagatesStreakCountCorrectly() =
        runTest {
            preferences.putString(PrefKeys.ID_TOKEN.name, "test-token")
            val result =
                buildUseCase(FakeCommonApis(dailyStreak = buildDailyStreak(streakCount = 42L)))
                    .invoke(FetchDailyStreakUseCase.Params(userPrincipal = "any-principal"))
            assertEquals(42L, result.unwrap().streakCount)
        }

    @Test
    fun propagatesStreakExpiresAtCorrectly() =
        runTest {
            preferences.putString(PrefKeys.ID_TOKEN.name, "test-token")
            val expiresAt = 9_007_199_254_740_991L
            val result =
                buildUseCase(FakeCommonApis(dailyStreak = buildDailyStreak(streakExpiresAtEpochMs = expiresAt)))
                    .invoke(FetchDailyStreakUseCase.Params(userPrincipal = "any-principal"))
            assertEquals(expiresAt, result.unwrap().streakExpiresAtEpochMs)
        }

    @Test
    fun forwardsUserPrincipalToCommonApis() =
        runTest {
            preferences.putString(PrefKeys.ID_TOKEN.name, "test-token")
            val fake = FakeCommonApis(dailyStreak = buildDailyStreak())
            buildUseCase(fake).invoke(FetchDailyStreakUseCase.Params(userPrincipal = "expected-principal"))
            assertEquals("expected-principal", fake.lastRequestedPrincipal)
        }

    @Test
    fun notifiesFailureListenerWhenCommonApisThrows() =
        runTest {
            preferences.putString(PrefKeys.ID_TOKEN.name, "test-token")
            val failureListener = RecordingUseCaseFailureListener()
            buildUseCase(ThrowingCommonApis(), failureListener)
                .invoke(FetchDailyStreakUseCase.Params(userPrincipal = "principal-1"))
            assertNotNull(failureListener.lastFailure)
        }

    @Test
    fun notifiesFailureListenerWhenIdTokenIsMissing() =
        runTest {
            val failureListener = RecordingUseCaseFailureListener()
            buildUseCase(FakeCommonApis(dailyStreak = buildDailyStreak()), failureListener)
                .invoke(FetchDailyStreakUseCase.Params(userPrincipal = "principal-1"))
            assertNotNull(failureListener.lastFailure)
        }

    @Test
    fun doesNotNotifyFailureListenerOnSuccess() =
        runTest {
            preferences.putString(PrefKeys.ID_TOKEN.name, "test-token")
            val failureListener = RecordingUseCaseFailureListener()
            buildUseCase(FakeCommonApis(dailyStreak = buildDailyStreak()), failureListener)
                .invoke(FetchDailyStreakUseCase.Params(userPrincipal = "principal-1"))
            assertNull(failureListener.lastFailure)
        }
}

private fun buildDailyStreak(
    streakCount: Long = 1L,
    streakExpiresAtEpochMs: Long = 1_000_000L,
) = DailyStreak(
    justIncremented = true,
    streakCount = streakCount,
    streakAction = "daily_login",
    streakExpiresAtEpochMs = streakExpiresAtEpochMs,
    nextIncrementEligibleAtEpochMs = 2_000_000L,
    serverNowEpochMs = 500_000L,
)

private class ThrowingCommonApis : CommonApis {
    override suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews> = emptyList()

    override suspend fun softDeleteInfluencer(
        principal: String,
        idToken: String,
        chatBaseUrl: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun fetchDailyStreak(
        userPrincipal: String,
        idToken: String,
    ): DailyStreak = throw RuntimeException("Network error")
}
