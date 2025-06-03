package com.yral.shared.features.root.viewmodels

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.feed.useCases.FetchFeedDetailsUseCase
import com.yral.shared.features.feed.useCases.GetInitialFeedUseCase
import com.yral.shared.firebaseStore.usecase.CheckVideoVoteUseCase
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.domain.models.PostResponse
import com.yral.shared.rust.services.IndividualUserServiceFactory
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for RootViewModel which handles the app's initialization flow and splash screen behavior.
 * Key responsibilities:
 * - Managing splash screen visibility
 * - Handling initialization delays and timeouts
 * - Loading initial feed content
 * - Error handling and retry functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {
    @MockK
    private lateinit var authClient: AuthClient

    @MockK
    private lateinit var sessionManager: SessionManager

    @MockK
    private lateinit var individualUserServiceFactory: IndividualUserServiceFactory

    @MockK
    private lateinit var getInitialFeedUseCase: GetInitialFeedUseCase

    @MockK
    private lateinit var fetchFeedDetailsUseCase: FetchFeedDetailsUseCase

    @MockK
    private lateinit var checkVideoVoteUseCase: CheckVideoVoteUseCase

    @MockK
    private lateinit var crashlyticsManager: CrashlyticsManager

    private val testDispatcher = StandardTestDispatcher()
    private val sessionState = MutableStateFlow<SessionState>(SessionState.Initial)

    private lateinit var viewModel: RootViewModel

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { sessionManager.state } returns sessionState
        every { sessionManager.getCanisterPrincipal() } returns CANISTER_PRINCIPAL
        every { sessionManager.getUserPrincipal() } returns USER_PRINCIPAL
        every { sessionManager.getIdentity() } returns IDENTITY_DATA
        coEvery { authClient.initialize() } returns Unit
        coEvery { individualUserServiceFactory.initialize(any(), any()) } returns Unit
        every { crashlyticsManager.recordException(any()) } returns Unit

        viewModel =
            RootViewModel(
                appDispatchers =
                    AppDispatchers(
                        io = testDispatcher,
                        main = testDispatcher,
                        mainImmediate = testDispatcher,
                    ),
                authClient = authClient,
                sessionManager = sessionManager,
                individualUserServiceFactory = individualUserServiceFactory,
                getInitialFeedUseCase = getInitialFeedUseCase,
                fetchFeedDetailsUseCase = fetchFeedDetailsUseCase,
                checkVideoVoteUseCase = checkVideoVoteUseCase,
                crashlyticsManager = crashlyticsManager,
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Tests the complete successful initialization flow:
     * 1. Shows splash screen initially
     * 2. Waits for initial setup delay
     * 3. Loads initial posts successfully
     * 4. Loads feed details for the first post
     * 5. Hides splash screen when complete
     *
     * Verifies:
     * - Correct state transitions
     * - Proper loading of posts and feed details
     * - Correct API calls with expected parameters
     */
    @Test
    fun `initialization flow completes successfully and shows feed screen`() =
        runTest {
            // Given
            val mockPosts = createMockPosts(5)
            val mockFeedDetails = createMockFeedDetails(mockPosts)

            coEvery {
                getInitialFeedUseCase.invoke(any())
            } returns Ok(PostResponse(posts = mockPosts))

            mockPosts.forEach { post ->
                coEvery {
                    fetchFeedDetailsUseCase.invoke(post)
                } returns Ok(createMockFeedDetails(post.videoID))
            }

            // When
            viewModel.state.test {
                // Initial state
                val initialState = awaitItem()
                assertTrue(initialState.showSplash)
                assertNull(initialState.error)

                // Start initialization
                viewModel.initialize()

                // Skip initial delay
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP)
                runCurrent()

                // State after session initialization
                val stateAfterSession = awaitItem()
                assertTrue(stateAfterSession.showSplash)
                assertNull(stateAfterSession.error)

                // Verify posts are loaded
                val stateWithPosts = awaitItem()
                assertEquals(stateWithPosts.posts.map { it.videoID }, mockPosts.map { it.videoID })
                assertTrue(stateWithPosts.showSplash)

                // Verify feed details are loaded
                val finalState = awaitItem()
                assertFalse(finalState.showSplash)
                assertEquals(
                    finalState.feedDetails.map { it.videoID },
                    mockFeedDetails.take(1).map { it.videoID },
                )
                assertNull(finalState.error)

                cancelAndIgnoreRemainingEvents()
            }

            // Then
            coVerify {
                getInitialFeedUseCase.invoke(
                    GetInitialFeedUseCase.Params(
                        canisterID = USER_PRINCIPAL,
                        filterResults = emptyList(),
                    ),
                )
            }
            coVerify(exactly = 1) {
                fetchFeedDetailsUseCase.invoke(mockPosts.first())
            }
        }

    /**
     * Tests error handling when initial feed loading fails:
     * 1. Shows splash screen initially
     * 2. Attempts to load initial feed
     * 3. Handles network error gracefully
     * 4. Shows appropriate error state
     *
     * Verifies:
     * - Error state is properly set
     * - Splash screen remains visible during error
     */
    @Test
    fun `initialization shows error when initial feed loading fails`() =
        runTest {
            // Given
            coEvery {
                getInitialFeedUseCase.invoke(any())
            } returns Err(Exception("Network error"))

            // When
            viewModel.state.test {
                // Initial state
                val initialState = awaitItem()
                assertTrue(initialState.showSplash)
                assertNull(initialState.error)

                // Start initialization
                viewModel.initialize()

                // Skip initial delay
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP)

                // State after session initialization
                val stateAfterSession = awaitItem()
                assertTrue(stateAfterSession.showSplash)
                assertNull(stateAfterSession.error)

                // Verify error state
                val errorState = awaitItem()
                assertEquals(RootError.INITIAL_CONTENT_FAILED, errorState.error)
                assertTrue(errorState.showSplash)
            }
        }

    /**
     * Tests timeout behavior during initialization:
     * 1. Shows splash screen initially
     * 2. Starts initialization that takes longer than timeout
     * 3. Triggers timeout before completion
     *
     * Verifies:
     * - Timeout error is shown after threshold
     * - Crashlytics records the timeout
     * - Splash screen remains visible
     */
    @Test
    fun `initialization times out after threshold`() =
        runTest {
            // Given
            coEvery {
                getInitialFeedUseCase.invoke(any())
            } coAnswers {
                // Simulate long-running operation that exceeds timeout
                delay(RootViewModel.SPLASH_SCREEN_TIMEOUT + 1000)
                Ok(PostResponse(posts = emptyList()))
            }

            // When
            viewModel.state.test {
                // Initial state
                val initialState = awaitItem()
                assertTrue(initialState.showSplash)
                assertNull(initialState.error)

                // Start initialization
                viewModel.initialize()

                // Skip initial delay
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP)

                // State after session initialization
                val stateAfterSession = awaitItem()
                assertTrue(stateAfterSession.showSplash)
                assertNull(stateAfterSession.error)

                // Skip to timeout
                advanceTimeBy(RootViewModel.SPLASH_SCREEN_TIMEOUT - RootViewModel.INITIAL_DELAY_FOR_SETUP)

                // Verify timeout error
                val timeoutState = awaitItem()
                assertEquals(RootError.TIMEOUT, timeoutState.error)
                assertTrue(timeoutState.showSplash)

                // Verify crashlytics was called
                coVerify { crashlyticsManager.recordException(any()) }
            }
        }

    /**
     * Tests retry functionality after an error:
     * 1. First attempt fails with network error
     * 2. Shows error state
     * 3. Retry attempt succeeds
     * 4. Completes initialization successfully
     *
     * Verifies:
     * - Error handling and recovery
     * - State transitions during retry
     * - Successful completion after retry
     */
    @Test
    fun `retry initialization after error works`() =
        runTest {
            // Given
            var shouldFail = true
            val mockPosts = List(5) { createMockPost(it.toString()) }
            val mockFeedDetails = List(5) { createMockFeedDetails(it.toString()) }

            coEvery {
                getInitialFeedUseCase.invoke(any())
            } answers {
                if (shouldFail) {
                    shouldFail = false
                    Err(Exception("First attempt fails"))
                } else {
                    Ok(PostResponse(posts = mockPosts))
                }
            }

            mockPosts.forEach { post ->
                coEvery {
                    fetchFeedDetailsUseCase.invoke(post)
                } returns Ok(createMockFeedDetails(post.videoID))
            }

            // When
            viewModel.state.test {
                // Initial state
                val initialState = awaitItem()
                assertTrue(initialState.showSplash)

                // First attempt - fails
                viewModel.initialize()
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP)
                runCurrent()

                // State after first session initialization
                val stateAfterFirstSession = awaitItem()
                assertTrue(stateAfterFirstSession.showSplash)
                assertNull(stateAfterFirstSession.error)

                // Error state
                val errorState = awaitItem()
                assertEquals(RootError.INITIAL_CONTENT_FAILED, errorState.error)

                // Retry - succeeds
                viewModel.initialize()
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP)
                runCurrent()

                // State after second session initialization
                val stateAfterSecondSession = awaitItem()
                assertTrue(stateAfterSecondSession.showSplash)
                assertNull(stateAfterSecondSession.error)

                // Skip states until final
                var currentState = awaitItem()
                while (currentState.showSplash) {
                    currentState = awaitItem()
                }

                // Verify successful state
                assertFalse(currentState.showSplash)
                assertNull(currentState.error)
                assertEquals(currentState.posts.map { it.videoID }, mockPosts.map { it.videoID })
                assertEquals(
                    currentState.feedDetails.map { it.videoID },
                    mockFeedDetails
                        .take(1)
                        .map { it.videoID },
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Tests that initialization respects the initial delay:
     * 1. Starts initialization
     * 2. Verifies no work is done before delay
     * 3. Confirms work starts after delay
     *
     * Verifies:
     * - Initial delay is respected
     * - No premature initialization
     * - Proper timing of initialization start
     */
    @Test
    fun `initialization waits for initial delay before starting`() =
        runTest {
            // Given
            var initializationStarted = false
            coEvery {
                getInitialFeedUseCase.invoke(any())
            } coAnswers {
                initializationStarted = true
                delay(100) // Add small delay to ensure timing check works
                Ok(PostResponse(posts = emptyList()))
            }

            // When
            viewModel.state.test {
                // Initial state
                val initialState = awaitItem()
                assertTrue(initialState.showSplash)

                // Start initialization
                viewModel.initialize()

                // Verify initialization hasn't started before delay
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP - 100)
                assertFalse(initializationStarted)

                // Verify initialization starts after delay
                advanceTimeBy(100)
                runCurrent() // Run any pending coroutines
                assertTrue(initializationStarted)

                // Verify final states
                val stateAfterDelay = awaitItem()
                assertTrue(stateAfterDelay.showSplash)
                assertNull(stateAfterDelay.error)

                // Consume final state
                val finalState = awaitItem()
                assertFalse(finalState.showSplash)
                assertNull(finalState.error)

                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Tests handling of multiple initialization calls:
     * 1. Starts first initialization
     * 2. Starts second initialization before first completes
     * 3. Both complete with proper delays
     *
     * Verifies:
     * - Each call respects delay
     * - Multiple calls handled properly
     * - State transitions for both calls
     */
    @Test
    fun `multiple initialization calls respect delay for each call`() =
        runTest {
            // Given
            var initializationCount = 0
            coEvery {
                getInitialFeedUseCase.invoke(any())
            } coAnswers {
                initializationCount++
                delay(100) // Add small delay to ensure timing check works
                Ok(PostResponse(posts = emptyList()))
            }

            // When
            viewModel.state.test {
                // Initial state
                val initialState = awaitItem()
                assertTrue(initialState.showSplash)

                // First initialization
                viewModel.initialize()
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP - 100)
                assertEquals(0, initializationCount)

                // Start second initialization before first one completes
                viewModel.initialize()

                // Complete first delay
                advanceTimeBy(100)
                runCurrent() // Run any pending coroutines
                assertEquals(0, initializationCount)

                // Complete second delay
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP)
                runCurrent() // Run any pending coroutines
                assertEquals(1, initializationCount)

                // Verify state transitions
                val stateAfterFirstInit = awaitItem()
                assertTrue(stateAfterFirstInit.showSplash)

                val stateAfterSecondInit = awaitItem()
                assertFalse(stateAfterSecondInit.showSplash)

                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Tests that initialization timeout is independent of initial delay:
     * 1. Starts initialization with delay
     * 2. Operation takes longer than timeout
     * 3. Timeout occurs at correct time
     *
     * Verifies:
     * - Timeout threshold is consistent
     * - Initial delay doesn't affect timeout
     * - Error handling for timeout
     */
    @Test
    fun `initialization delay does not affect timeout threshold`() =
        runTest {
            // Given
            coEvery {
                getInitialFeedUseCase.invoke(any())
            } coAnswers {
                delay(RootViewModel.SPLASH_SCREEN_TIMEOUT - RootViewModel.INITIAL_DELAY_FOR_SETUP + 100)
                Ok(PostResponse(posts = emptyList()))
            }

            // When
            viewModel.state.test {
                // Initial state
                val initialState = awaitItem()
                assertTrue(initialState.showSplash)
                assertNull(initialState.error)

                // Start initialization
                viewModel.initialize()

                // Skip initial delay
                advanceTimeBy(RootViewModel.INITIAL_DELAY_FOR_SETUP)
                runCurrent() // Run any pending coroutines

                // State after session initialization
                val stateAfterSession = awaitItem()
                assertTrue(stateAfterSession.showSplash)
                assertNull(stateAfterSession.error)

                // Skip to just before timeout
                advanceTimeBy(RootViewModel.SPLASH_SCREEN_TIMEOUT - RootViewModel.INITIAL_DELAY_FOR_SETUP - 100)
                runCurrent() // Run any pending coroutines

                // Skip to timeout
                advanceTimeBy(100)
                runCurrent() // Run any pending coroutines

                // Verify timeout error
                val timeoutState = awaitItem()
                assertEquals(RootError.TIMEOUT, timeoutState.error)
                assertTrue(timeoutState.showSplash)

                // Verify crashlytics was called
                coVerify { crashlyticsManager.recordException(any()) }

                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val CANISTER_PRINCIPAL = "test-canister-principal"
        const val USER_PRINCIPAL = "test-user-principal"
        val IDENTITY_DATA = "test-identity".toByteArray()

        // Cache for mocks to ensure consistent object identity
        private val mockCache = mutableMapOf<String, Any>()

        fun createMockPost(id: String): Post =
            mockCache.getOrPut("post_$id") {
                mockk<Post>(relaxed = true).also { mock ->
                    every { mock.videoID } returns id
                    every { mock.toString() } returns "Post(#$id)"
                    every { mock.equals(any()) } answers {
                        val other = firstArg<Any>()
                        if (other !is Post) {
                            false
                        } else {
                            other.videoID == mock.videoID
                        }
                    }
                    every { mock.hashCode() } returns id.hashCode()
                }
            } as Post

        fun createMockFeedDetails(id: String): FeedDetails =
            mockCache.getOrPut("feed_$id") {
                mockk<FeedDetails>(relaxed = true).also { mock ->
                    every { mock.videoID } returns id
                    every { mock.toString() } returns "FeedDetails(#$id)"
                    every { mock == any() } answers {
                        val other = firstArg<Any>()
                        if (other !is FeedDetails) {
                            false
                        } else {
                            other.videoID == mock.videoID
                        }
                    }
                    every { mock.hashCode() } returns id.hashCode()
                }
            } as FeedDetails

        fun createMockPosts(count: Int) = List(count) { createMockPost(it.toString()) }
        fun createMockFeedDetails(posts: List<Post>) =
            posts
                .map { createMockFeedDetails(it.videoID) }
    }
}
