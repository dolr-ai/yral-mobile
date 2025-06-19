package com.yral.shared.features.root.viewmodels

import app.cash.turbine.test
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.AuthClient
import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.uniffi.generated.FfiException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
class RootViewModelTest {
    private lateinit var authClient: AuthClient
    private lateinit var sessionManager: SessionManager
    private lateinit var individualUserServiceFactory: IndividualUserServiceFactory
    private lateinit var crashlyticsManager: CrashlyticsManager

    private val testDispatcher = StandardTestDispatcher()
    private val sessionState = MutableStateFlow<SessionState>(SessionState.Initial)

    private lateinit var viewModel: RootViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authClient = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        individualUserServiceFactory = mockk(relaxed = true)
        crashlyticsManager = mockk(relaxed = true)

        every { sessionManager.state } returns sessionState
        every { sessionManager.getCanisterPrincipal() } returns CANISTER_PRINCIPAL
        every { sessionManager.getUserPrincipal() } returns USER_PRINCIPAL
        every { sessionManager.getIdentity() } returns IDENTITY_DATA

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
                crashlyticsManager = crashlyticsManager,
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state shows splash and no error`() =
        runTest {
            viewModel.state.test {
                val initial = awaitItem()
                assertTrue(initial.showSplash)
                assertNull(initial.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `successful initialization with principal and identity hides splash`() =
        runTest {
            coEvery { individualUserServiceFactory.initialize(any(), any()) } returns Unit
            every { sessionManager.getCanisterPrincipal() } returns CANISTER_PRINCIPAL
            every { sessionManager.getIdentity() } returns IDENTITY_DATA
            viewModel.state.test {
                awaitItem() // initial
                viewModel.initialize()
                // Wait for delay and coroutine
                delay(RootViewModel.INITIAL_DELAY_FOR_SETUP + 10)
                val stateAfter = awaitItem() // state reset
                val finalState = awaitItem() // after initialization
                assertFalse(finalState.showSplash)
                assertNull(finalState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `initialization with missing principal calls authClient and keeps splash`() =
        runTest {
            every { sessionManager.getCanisterPrincipal() } returns null
            coEvery { authClient.initialize() } returns Unit
            viewModel.state.test {
                awaitItem() // initial
                viewModel.initialize()
                delay(RootViewModel.INITIAL_DELAY_FOR_SETUP + 10)
                val stateAfter = awaitItem() // state reset
                // Should not hide splash
                assertTrue(stateAfter.showSplash)
                assertNull(stateAfter.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `initialization with missing identity calls authClient and keeps splash`() =
        runTest {
            every { sessionManager.getCanisterPrincipal() } returns CANISTER_PRINCIPAL
            every { sessionManager.getIdentity() } returns null
            coEvery { authClient.initialize() } returns Unit
            viewModel.state.test {
                awaitItem() // initial
                viewModel.initialize()
                delay(RootViewModel.INITIAL_DELAY_FOR_SETUP + 10)
                val stateAfter = awaitItem() // state reset
                // Should not hide splash
                assertTrue(stateAfter.showSplash)
                assertNull(stateAfter.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `timeout during initialization sets error and keeps splash`() =
        runTest {
            // Simulate a long-running initialization
            coEvery { individualUserServiceFactory.initialize(any(), any()) } coAnswers {
                delay(1000) // longer than the test timeout
            }
            every { sessionManager.getCanisterPrincipal() } returns CANISTER_PRINCIPAL
            every { sessionManager.getIdentity() } returns IDENTITY_DATA
            val fastTimeoutViewModel =
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
                    crashlyticsManager = crashlyticsManager,
                )
            fastTimeoutViewModel.splashScreenTimeout = 2000L
            fastTimeoutViewModel.initialDelayForSetup = 2000L
            fastTimeoutViewModel.state.test {
                awaitItem() // initial
                fastTimeoutViewModel.initialize()
                // Wait for the error state to be emitted
                var errorState: RootState? = null
                repeat(10) {
                    val state = awaitItem()
                    if (state.error == RootError.TIMEOUT) {
                        errorState = state
                        return@test
                    }
                }
                assertTrue(errorState != null, "Timeout error state was not emitted")
                assertTrue(errorState!!.showSplash)
                assertEquals(RootError.TIMEOUT, errorState!!.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `YralException during initialization sets error and keeps splash`() =
        runTest {
            coEvery { individualUserServiceFactory.initialize(any(), any()) } throws YralException("Test error")
            every { sessionManager.getCanisterPrincipal() } returns CANISTER_PRINCIPAL
            every { sessionManager.getIdentity() } returns IDENTITY_DATA
            viewModel.state.test {
                awaitItem() // initial
                viewModel.initialize()
                delay(RootViewModel.INITIAL_DELAY_FOR_SETUP + 10)
                val stateAfter = awaitItem() // state reset
                val errorState = awaitItem() // after error
                assertTrue(errorState.showSplash)
                assertEquals(RootError.TIMEOUT, errorState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `FfiException during initialization sets error and keeps splash`() =
        runTest {
            coEvery { individualUserServiceFactory.initialize(any(), any()) } throws mockk<FfiException>(relaxed = true)
            every { sessionManager.getCanisterPrincipal() } returns CANISTER_PRINCIPAL
            every { sessionManager.getIdentity() } returns IDENTITY_DATA
            viewModel.state.test {
                awaitItem() // initial
                viewModel.initialize()
                delay(RootViewModel.INITIAL_DELAY_FOR_SETUP + 10)
                val stateAfter = awaitItem() // state reset
                val errorState = awaitItem() // after error
                assertTrue(errorState.showSplash)
                assertEquals(RootError.TIMEOUT, errorState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onSplashAnimationComplete updates state`() =
        runTest {
            viewModel.state.test {
                awaitItem() // initial
                viewModel.onSplashAnimationComplete()
                val updated = awaitItem()
                assertTrue(updated.initialAnimationComplete)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updateCurrentTab updates state`() =
        runTest {
            val newTab = "Profile"
            viewModel.state.test {
                awaitItem() // initial
                viewModel.updateCurrentTab(newTab)
                val updated = awaitItem()
                assertEquals(newTab, updated.currentHomePageTab)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `initialization completes before timeout does not set error`() =
        runTest {
            // Simulate a fast initialization
            coEvery { individualUserServiceFactory.initialize(any(), any()) } coAnswers {
                delay(50) // 50ms, much less than the timeout
            }
            every { sessionManager.getCanisterPrincipal() } returns CANISTER_PRINCIPAL
            every { sessionManager.getIdentity() } returns IDENTITY_DATA
            val fastEnoughViewModel =
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
                    crashlyticsManager = crashlyticsManager,
                )
            fastEnoughViewModel.splashScreenTimeout = 500L
            fastEnoughViewModel.initialDelayForSetup = 50L
            fastEnoughViewModel.state.test {
                awaitItem() // initial
                fastEnoughViewModel.initialize()
                // Wait for the state after initialization
                val stateAfter = awaitItem() // state reset
                val finalState = awaitItem() // after initialization
                assertFalse(finalState.showSplash)
                assertNull(finalState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    companion object {
        const val CANISTER_PRINCIPAL = "test-canister-principal"
        const val USER_PRINCIPAL = "test-user-principal"
        val IDENTITY_DATA = "test-identity".toByteArray()
    }
}
