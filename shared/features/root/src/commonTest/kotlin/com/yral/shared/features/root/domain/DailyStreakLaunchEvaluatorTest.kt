package com.yral.shared.features.root.domain

import com.yral.shared.preferences.stores.DailyStreakLaunchStore
import com.yral.shared.testsupport.preferences.FakePreferences
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyStreakLaunchEvaluatorTest {
    private val preferences = FakePreferences()
    private val store = DailyStreakLaunchStore(preferences)
    private val evaluator = DailyStreakLaunchEvaluator(store)

    @Test
    fun `first observed streak stores baseline without showing`() =
        runTest {
            val result =
                evaluator.evaluate(
                    principal = "principal-a",
                    remoteStreakCount = 1L,
                )

            assertEquals(DailyStreakLaunchResult.NoChange, result)
            assertEquals(1L, store.getStreakCount("principal-a"))
        }

    @Test
    fun `increased streak shows celebration and persists new count`() =
        runTest {
            store.putStreakCount("principal-a", 3L)

            val result =
                evaluator.evaluate(
                    principal = "principal-a",
                    remoteStreakCount = 4L,
                )

            assertEquals(DailyStreakLaunchResult.ShowCelebration(4L), result)
            assertEquals(4L, store.getStreakCount("principal-a"))
        }

    @Test
    fun `unchanged streak does not show celebration`() =
        runTest {
            store.putStreakCount("principal-a", 4L)

            val result =
                evaluator.evaluate(
                    principal = "principal-a",
                    remoteStreakCount = 4L,
                )

            assertEquals(DailyStreakLaunchResult.NoChange, result)
            assertEquals(4L, store.getStreakCount("principal-a"))
        }

    @Test
    fun `decreased streak updates local baseline without showing`() =
        runTest {
            store.putStreakCount("principal-a", 5L)

            val result =
                evaluator.evaluate(
                    principal = "principal-a",
                    remoteStreakCount = 1L,
                )

            assertEquals(DailyStreakLaunchResult.NoChange, result)
            assertEquals(1L, store.getStreakCount("principal-a"))
        }

    @Test
    fun `streak counts remain isolated per principal`() =
        runTest {
            store.putStreakCount("principal-a", 2L)
            store.putStreakCount("principal-b", 7L)

            val result =
                evaluator.evaluate(
                    principal = "principal-a",
                    remoteStreakCount = 3L,
                )

            assertEquals(DailyStreakLaunchResult.ShowCelebration(3L), result)
            assertEquals(3L, store.getStreakCount("principal-a"))
            assertEquals(7L, store.getStreakCount("principal-b"))
        }
}
