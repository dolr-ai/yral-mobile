@file:OptIn(ExperimentalCoroutinesApi::class)

package com.yral.shared.features.wallet.viewmodel

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.wallet.analytics.WalletTelemetry
import com.yral.shared.features.wallet.domain.models.BillingBalance
import com.yral.shared.features.wallet.domain.models.Transaction
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeBillingUseCase: FakeBillingBalanceUseCase
    private lateinit var fakeTransactionsUseCase: FakeTransactionsUseCase
    private lateinit var fakeMetadataDataSource: FakeFollowersMetadataDataSource

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionManager = SessionManager()
        fakeBillingUseCase = FakeBillingBalanceUseCase(testDispatcher)
        fakeTransactionsUseCase = FakeTransactionsUseCase(testDispatcher)
        fakeMetadataDataSource = FakeFollowersMetadataDataSource()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): WalletViewModel =
        WalletViewModel(
            sessionManager = sessionManager,
            walletTelemetry = WalletTelemetry(AnalyticsManager()),
            getBillingBalanceUseCase = fakeBillingUseCase,
            getTransactionsUseCase = fakeTransactionsUseCase,
            metadataDataSource = fakeMetadataDataSource,
        )

    private fun signInUser(userPrincipal: String = "test-principal-123") {
        sessionManager.updateState(
            SessionState.SignedIn(Session(userPrincipal = userPrincipal)),
        )
    }

    // region initial state

    @Test
    fun `initial state has empty earnings and not signed in`() =
        runTest {
            val viewModel = createViewModel()
            val state = viewModel.state.value

            assertEquals("", state.totalEarningsInr)
            assertFalse(state.isSocialSignedIn)
            assertFalse(state.hasBots)
            assertFalse(state.isLoading)
            assertTrue(state.transactions.isEmpty())
        }

    // endregion

    // region balance fetching and display

    @Test
    fun `balance is fetched and displayed when social signed in`() =
        runTest {
            fakeBillingUseCase.result =
                Ok(
                    BillingBalance(balancePaise = 50000L, balanceRupees = 500.0),
                )
            signInUser()
            val viewModel = createViewModel()

            sessionManager.updateSocialSignInStatus(true)
            advanceUntilIdle()

            assertEquals("₹500.0", viewModel.state.value.totalEarningsInr)
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun `balance shows zero on fetch failure`() =
        runTest {
            fakeBillingUseCase.result = Err(RuntimeException("API error"))
            signInUser()
            val viewModel = createViewModel()

            sessionManager.updateSocialSignInStatus(true)
            advanceUntilIdle()

            assertEquals("₹0", viewModel.state.value.totalEarningsInr)
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun `wallet data not loaded when userPrincipal is null`() =
        runTest {
            // Don't call signInUser — userPrincipal stays null
            val viewModel = createViewModel()

            sessionManager.updateSocialSignInStatus(true)
            advanceUntilIdle()

            assertEquals("", viewModel.state.value.totalEarningsInr)
            assertTrue(
                viewModel.state.value.transactions
                    .isEmpty(),
            )
        }

    // endregion

    // region transaction fetching

    @Test
    fun `transactions are loaded when social signed in`() =
        runTest {
            val testTransactions =
                listOf(
                    createTransaction(id = "tx-1", userId = "user-1", amountPaise = 10000L),
                    createTransaction(id = "tx-2", userId = "user-2", amountPaise = 20000L),
                )
            fakeTransactionsUseCase.result = Ok(testTransactions)
            fakeMetadataDataSource.usernameMap =
                mapOf(
                    "user-1" to "alice",
                    "user-2" to "bob",
                )
            signInUser()
            val viewModel = createViewModel()

            sessionManager.updateSocialSignInStatus(true)
            advanceUntilIdle()

            val transactions = viewModel.state.value.transactions
            assertEquals(2, transactions.size)
            assertEquals("tx-1", transactions[0].id)
            assertEquals("user-1", transactions[0].userId)
            assertEquals(10000L, transactions[0].amountPaise)
            assertEquals("tx-2", transactions[1].id)
        }

    @Test
    fun `transaction fetch failure leaves empty list`() =
        runTest {
            fakeTransactionsUseCase.result = Err(RuntimeException("Network error"))
            signInUser()
            val viewModel = createViewModel()

            sessionManager.updateSocialSignInStatus(true)
            advanceUntilIdle()

            assertTrue(
                viewModel.state.value.transactions
                    .isEmpty(),
            )
            assertFalse(viewModel.state.value.isTransactionsLoading)
        }

    // endregion

    // region username resolution

    @Test
    fun `usernames are resolved from metadata after transaction fetch`() =
        runTest {
            fakeTransactionsUseCase.result =
                Ok(
                    listOf(
                        createTransaction(id = "tx-1", userId = "principal-abc"),
                        createTransaction(id = "tx-2", userId = "principal-xyz"),
                    ),
                )
            fakeMetadataDataSource.usernameMap =
                mapOf(
                    "principal-abc" to "alice",
                    "principal-xyz" to "bob",
                )
            signInUser()
            val viewModel = createViewModel()

            sessionManager.updateSocialSignInStatus(true)
            advanceUntilIdle()

            val transactions = viewModel.state.value.transactions
            assertEquals(2, transactions.size)
            assertEquals("alice", transactions[0].username)
            assertEquals("bob", transactions[1].username)
        }

    @Test
    fun `username resolution failure keeps transactions in state`() =
        runTest {
            fakeTransactionsUseCase.result =
                Ok(
                    listOf(createTransaction(id = "tx-1", userId = "principal-abc")),
                )
            fakeMetadataDataSource.shouldThrow = true
            signInUser()
            val viewModel = createViewModel()

            sessionManager.updateSocialSignInStatus(true)
            advanceUntilIdle()

            val transactions = viewModel.state.value.transactions
            assertEquals(1, transactions.size)
            assertEquals("tx-1", transactions[0].id)
        }

    // endregion

    // region session state

    @Test
    fun `hasBots is true when botCount is positive`() =
        runTest {
            val viewModel = createViewModel()

            sessionManager.updateBotCount(2)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.hasBots)
        }

    @Test
    fun `hasBots is false when botCount is null`() =
        runTest {
            val viewModel = createViewModel()

            sessionManager.updateBotCount(null)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.hasBots)
        }

    @Test
    fun `hasBots is false when botCount is zero`() =
        runTest {
            val viewModel = createViewModel()

            sessionManager.updateBotCount(0)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.hasBots)
        }

    // endregion

    // region UI state toggles

    @Test
    fun `toggleHowToEarnHelp updates visibility`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.toggleHowToEarnHelp(true)
            assertTrue(viewModel.state.value.howToEarnVisible)

            viewModel.toggleHowToEarnHelp(false)
            assertFalse(viewModel.state.value.howToEarnVisible)
        }

    @Test
    fun `toggleTransactionHistory updates visibility`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.toggleTransactionHistory(true)
            assertTrue(viewModel.state.value.showTransactionHistory)

            viewModel.toggleTransactionHistory(false)
            assertFalse(viewModel.state.value.showTransactionHistory)
        }

    // endregion

    // region helpers

    private fun createTransaction(
        id: String = "tx-1",
        userId: String = "user-principal-1",
        recipientId: String = "recipient-1",
        transactionType: String = "SUBSCRIPTION",
        amountPaise: Long = 10000L,
        createdAt: String = "2024-01-15T14:30:00Z",
    ) = Transaction(
        id = id,
        userId = userId,
        recipientId = recipientId,
        transactionType = transactionType,
        amountPaise = amountPaise,
        createdAt = createdAt,
        username = null,
    )

    // endregion
}

// region fake use cases

private class FakeBillingBalanceUseCase(
    testDispatcher: kotlinx.coroutines.CoroutineDispatcher,
) : SuspendUseCase<String, BillingBalance>(testDispatcher, NoOpUseCaseFailureListener()) {
    var result: Result<BillingBalance, Throwable> =
        Ok(
            BillingBalance(balancePaise = 0L, balanceRupees = 0.0),
        )

    override suspend fun execute(parameter: String): BillingBalance = throw NotImplementedError("Use executeWith instead")

    override suspend fun executeWith(parameter: String): Result<BillingBalance, Throwable> = result
}

private class FakeTransactionsUseCase(
    testDispatcher: kotlinx.coroutines.CoroutineDispatcher,
) : SuspendUseCase<String, List<Transaction>>(testDispatcher, NoOpUseCaseFailureListener()) {
    var result: Result<List<Transaction>, Throwable> = Ok(emptyList())

    override suspend fun execute(parameter: String): List<Transaction> = throw NotImplementedError("Use executeWith instead")

    override suspend fun executeWith(parameter: String): Result<List<Transaction>, Throwable> = result
}

// endregion

// region fakes

private class FakeFollowersMetadataDataSource : FollowersMetadataDataSource {
    var usernameMap: Map<String, String> = emptyMap()
    var shouldThrow: Boolean = false

    override suspend fun fetchUsernames(principals: List<String>): Map<String, String> {
        if (shouldThrow) throw RuntimeException("Metadata fetch failed")
        return usernameMap
    }
}

private class NoOpUseCaseFailureListener : UseCaseFailureListener {
    override fun onFailure(
        throwable: Throwable,
        tag: String?,
        message: () -> String,
        exceptionType: String?,
    ) {
        // no-op for tests
    }
}

// endregion
