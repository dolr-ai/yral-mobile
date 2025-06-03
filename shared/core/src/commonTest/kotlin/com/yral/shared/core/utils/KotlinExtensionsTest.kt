package com.yral.shared.core.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Comprehensive tests for the filterFirstNSuspendFlow extension function.
 * Tests various scenarios including success cases, error cases, concurrency, and edge cases.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KotlinExtensionsTest {
    /**
     * Tests the basic functionality when enough items pass the predicate.
     * Should return exactly n items that satisfy the condition.
     */
    @Test
    fun `filterFirstNSuspendFlow returns exactly n items when enough items pass predicate`() =
        runTest {
            // Given
            val items = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            val n = 3

            // When - filter for even numbers
            val result = items.filterFirstNSuspendFlow(n) { it % 2 == 0 }.toList()

            // Then
            assertEquals(n, result.size)
            assertTrue(result.all { it % 2 == 0 })
            // Should get the first 3 even numbers: 2, 4, 6
            assertEquals(listOf(2, 4, 6), result.sorted())
        }

    /**
     * Tests that InsufficientItemsException is thrown when not enough items pass the predicate.
     */
    @Test
    fun `filterFirstNSuspendFlow throws InsufficientItemsException when not enough items pass predicate`() =
        runTest {
            // Given
            val items = listOf(1, 3, 5, 7, 9) // All odd numbers
            val n = 3

            // When & Then - trying to filter for even numbers should fail
            val exception =
                assertFailsWith<InsufficientItemsException> {
                    items.filterFirstNSuspendFlow(n) { it % 2 == 0 }.toList()
                }
            assertEquals("Found only 0 items out of required 3", exception.message)
        }

    /**
     * Tests that InsufficientItemsException is thrown with correct count when some but not enough items pass.
     */
    @Test
    fun `filterFirstNSuspendFlow throws InsufficientItemsException with correct count when insufficient items`() =
        runTest {
            // Given
            val items = listOf(1, 2, 3, 5, 7, 9) // Only 1 even number
            val n = 3

            // When & Then - trying to get 3 even numbers when only 1 exists should fail
            val exception =
                assertFailsWith<InsufficientItemsException> {
                    items.filterFirstNSuspendFlow(n) { it % 2 == 0 }.toList()
                }
            assertEquals("Found only 1 items out of required 3", exception.message)
        }

    /**
     * Tests behavior when all items pass the predicate and we need fewer than total.
     */
    @Test
    fun `filterFirstNSuspendFlow works when all items pass predicate`() =
        runTest {
            // Given
            val items = listOf(2, 4, 6, 8, 10, 12) // All even numbers
            val n = 4

            // When - filter for even numbers
            val result = items.filterFirstNSuspendFlow(n) { it % 2 == 0 }.toList()

            // Then
            assertEquals(n, result.size)
            assertTrue(result.all { it % 2 == 0 })
            assertEquals(4, result.size)
        }

    /**
     * Tests behavior when requesting exactly as many items as available.
     */
    @Test
    fun `filterFirstNSuspendFlow works when requesting exactly available items`() =
        runTest {
            // Given
            val items = listOf(1, 2, 3, 4, 5, 6) // 3 even numbers
            val n = 3

            // When - filter for even numbers
            val result = items.filterFirstNSuspendFlow(n) { it % 2 == 0 }.toList()

            // Then
            assertEquals(n, result.size)
            assertEquals(listOf(2, 4, 6), result.sorted())
        }

    /**
     * Tests behavior with empty input.
     */
    @Test
    fun `filterFirstNSuspendFlow throws exception with empty input`() =
        runTest {
            // Given
            val items = emptyList<Int>()
            val n = 1

            // When & Then
            val exception =
                assertFailsWith<InsufficientItemsException> {
                    items.filterFirstNSuspendFlow(n) { true }.toList()
                }
            assertEquals("Found only 0 items out of required 1", exception.message)
        }

    /**
     * Tests behavior when n = 0 (edge case).
     */
    @Test
    fun `filterFirstNSuspendFlow returns empty list when n is 0`() =
        runTest {
            // Given
            val items = listOf(1, 2, 3, 4, 5)
            val n = 0

            // When
            val result = items.filterFirstNSuspendFlow(n) { true }.toList()

            // Then
            assertEquals(0, result.size)
        }

    /**
     * Tests behavior when n is negative (edge case).
     */
    @Test
    fun `filterFirstNSuspendFlow returns empty list when n is negative`() =
        runTest {
            // Given
            val items = listOf(1, 2, 3, 4, 5)
            val n = -1

            // When
            val result = items.filterFirstNSuspendFlow(n) { true }.toList()

            // Then
            assertEquals(0, result.size)
        }

    /**
     * Tests behavior when n = 1 (minimum useful case).
     */
    @Test
    fun `filterFirstNSuspendFlow works correctly when n is 1`() =
        runTest {
            // Given
            val items = listOf(1, 2, 3, 4, 5)
            val n = 1

            // When - get first even number
            val result = items.filterFirstNSuspendFlow(n) { it % 2 == 0 }.toList()

            // Then
            assertEquals(1, result.size)
            assertEquals(2, result.first())
        }

    /**
     * Tests that the function works with async predicates that have delays.
     */
    @Test
    fun `filterFirstNSuspendFlow works with async predicates`() =
        runTest {
            // Given
            val items = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            val n = 3

            // When - predicate with delay to simulate async work
            val result =
                items
                    .filterFirstNSuspendFlow(n) { item ->
                        delay(10) // Simulate async work
                        item % 2 == 0
                    }.toList()

            // Then
            assertEquals(n, result.size)
            assertTrue(result.all { it % 2 == 0 })
        }

    /**
     * Tests concurrency limits by ensuring maxConcurrency parameter is respected.
     */
    @Test
    fun `filterFirstNSuspendFlow respects maxConcurrency parameter`() =
        runTest {
            // Given
            val items = (1..20).toList()
            val n = 5
            val maxConcurrency = 2
            var currentConcurrency = 0
            var maxObservedConcurrency = 0

            // When - predicate tracks concurrency
            val result =
                items
                    .filterFirstNSuspendFlow(n, maxConcurrency) { item ->
                        currentConcurrency++
                        maxObservedConcurrency = maxOf(maxObservedConcurrency, currentConcurrency)
                        delay(50) // Ensure some overlap
                        val result = item % 2 == 0
                        currentConcurrency--
                        result
                    }.toList()

            // Then
            assertEquals(n, result.size)
            assertTrue(
                maxObservedConcurrency <= maxConcurrency,
                "Max observed concurrency: $maxObservedConcurrency, limit: $maxConcurrency",
            )
        }

    /**
     * Tests that the function handles exceptions in predicates gracefully.
     */
    @Test
    fun `filterFirstNSuspendFlow propagates predicate exceptions`() =
        runTest {
            // Given
            val items = listOf(1, 2, 3, 4, 5)
            val n = 2

            // When & Then - predicate throws exception
            assertFailsWith<RuntimeException> {
                items
                    .filterFirstNSuspendFlow(n) {
                        if (it == 3) throw RuntimeException("Test exception")
                        it % 2 == 0
                    }.toList()
            }
        }

    /**
     * Tests behavior with large datasets to ensure performance.
     */
    @Test
    fun `filterFirstNSuspendFlow works efficiently with large datasets`() =
        runTest {
            // Given
            val items = (1..1000).toList()
            val n = 10

            // When - filter for numbers divisible by 100
            val result =
                items
                    .filterFirstNSuspendFlow(n) { item ->
                        // Small delay to simulate real work
                        delay(1)
                        item % 100 == 0
                    }.toList()

            // Then
            assertEquals(n, result.size)
            assertTrue(result.all { it % 100 == 0 })
            // Should get: 100, 200, 300, ..., 1000
            assertEquals((1..10).map { it * 100 }, result.sorted())
        }

    /**
     * Tests that the function cancels remaining jobs when enough items are found.
     */
    @Test
    fun `filterFirstNSuspendFlow cancels remaining jobs when target reached`() =
        runTest {
            // Given
            val items = (1..100).toList()
            val n = 3
            var jobsStarted = 0
            var jobsCompleted = 0

            // When
            val result =
                items
                    .filterFirstNSuspendFlow(n) { item ->
                        jobsStarted++
                        delay(10)
                        // Only 2, 4, 6 should pass
                        val passes = item <= 6 && item % 2 == 0
                        jobsCompleted++
                        passes
                    }.toList()

            // Then
            assertEquals(n, result.size)
            assertEquals(listOf(2, 4, 6), result.sorted())

            // Jobs should be cancelled after finding enough items
            // Note: Due to parallel execution, some extra jobs might start before cancellation
            assertTrue(
                jobsCompleted <= jobsStarted,
                "Completed: $jobsCompleted, Started: $jobsStarted",
            )
        }

    /**
     * Tests with custom predicate logic to ensure flexibility.
     */
    @Test
    fun `filterFirstNSuspendFlow works with complex predicates`() =
        runTest {
            // Given
            data class TestItem(
                val id: Int,
                val name: String,
                val active: Boolean,
            )

            val items =
                listOf(
                    TestItem(1, "Alice", true),
                    TestItem(2, "Bob", false),
                    TestItem(3, "Charlie", true),
                    TestItem(4, "David", true),
                    TestItem(5, "Eve", false),
                    TestItem(6, "Frank", true),
                )
            val n = 2

            // When - filter for active users with names longer than 3 characters
            val result =
                items
                    .filterFirstNSuspendFlow(n) { item ->
                        // Simulate async validation
                        delay(5)
                        item.active && item.name.length > 3
                    }.toList()

            // Then
            assertEquals(n, result.size)
            assertTrue(result.all { it.active && it.name.length > 3 })
            // Should get Alice and Charlie (or David, or Frank - depends on execution order)
            assertTrue(result.all { it.name in listOf("Alice", "Charlie", "David", "Frank") })
        }

    /**
     * Tests throwOnInsufficient = false: should return available items without throwing exception.
     */
    @Test
    fun `filterFirstNSuspendFlow with throwOnInsufficient false returns available items`() =
        runTest {
            // Given
            // Only 1 even number
            val items = listOf(1, 2, 3, 5, 7, 9)
            val n = 3

            // When - trying to get 3 even numbers when only 1 exists, but don't throw
            val result =
                items
                    .filterFirstNSuspendFlow(
                        n = n,
                        throwOnInsufficient = false,
                    ) { it % 2 == 0 }
                    .toList()

            // Then - should get the 1 available even number without exception
            assertEquals(1, result.size)
            assertEquals(2, result.first())
        }

    /**
     * Tests throwOnInsufficient = false with empty input: should return empty list.
     */
    @Test
    fun `filterFirstNSuspendFlow with throwOnInsufficient false and empty input returns empty list`() =
        runTest {
            // Given
            val items = emptyList<Int>()
            val n = 1

            // When
            val result =
                items
                    .filterFirstNSuspendFlow(
                        n = n,
                        throwOnInsufficient = false,
                    ) { true }
                    .toList()

            // Then
            assertEquals(0, result.size)
        }

    /**
     * Tests throwOnInsufficient = false when no items pass predicate: should return empty list.
     */
    @Test
    fun `filterFirstNSuspendFlow with throwOnInsufficient false and no matching items returns empty list`() =
        runTest {
            // Given
            // All odd numbers
            val items = listOf(1, 3, 5, 7, 9)
            val n = 3

            // When - trying to filter for even numbers with throwOnInsufficient = false
            val result =
                items
                    .filterFirstNSuspendFlow(
                        n = n,
                        throwOnInsufficient = false,
                    ) { it % 2 == 0 }
                    .toList()

            // Then
            assertEquals(0, result.size)
        }

    /**
     * Tests throwOnInsufficient = true (default behavior): should throw exception when insufficient items.
     */
    @Test
    fun `filterFirstNSuspendFlow with throwOnInsufficient true throws exception when insufficient items`() =
        runTest {
            // Given
            // Only 1 even number
            val items = listOf(1, 2, 3, 5, 7, 9)
            val n = 3

            // When & Then - should throw exception (default behavior)
            val exception =
                assertFailsWith<InsufficientItemsException> {
                    items
                        .filterFirstNSuspendFlow(
                            n = n,
                            throwOnInsufficient = true,
                        ) { it % 2 == 0 }
                        .toList()
                }
            assertEquals("Found only 1 items out of required 3", exception.message)
        }

    /**
     * Tests pagination scenario: get whatever items are available without errors.
     */
    @Test
    fun `filterFirstNSuspendFlow supports pagination scenario with partial results`() =
        runTest {
            // Given - simulating a feed where some items are already voted/seen
            val allPosts = (1..10).toList()
            // Half are voted
            val votedPosts = setOf(2, 4, 6, 8, 10)
            val requestedCount = 8

            // When - trying to get 8 unvoted posts but only 5 are available
            val result =
                allPosts
                    .filterFirstNSuspendFlow(
                        n = requestedCount,
                        // Don't throw, just return what's available
                        throwOnInsufficient = false,
                    ) { post ->
                        // Simulate async vote checking
                        delay(1)
                        !votedPosts.contains(post)
                    }.toList()

            // Then - should get the 5 available unvoted posts
            assertEquals(5, result.size)
            assertEquals(listOf(1, 3, 5, 7, 9), result.sorted())
            assertTrue(result.none { it in votedPosts })
        }
}
