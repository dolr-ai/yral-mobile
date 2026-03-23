package com.yral.shared.features.wallet.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.wallet.data.models.BillingBalanceDataDto
import com.yral.shared.features.wallet.data.models.BillingBalanceResponseDto
import com.yral.shared.features.wallet.data.models.BillingTransactionsResponseDto
import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto
import com.yral.shared.features.wallet.data.models.BtcRewardConfigResponseDto
import com.yral.shared.features.wallet.data.models.DolrPriceResponseDto
import com.yral.shared.features.wallet.data.models.GetBalanceResponseDto
import com.yral.shared.features.wallet.data.models.TransactionResponseDto
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WalletRepositoryImplTest {
    private lateinit var fakeDataSource: FakeWalletDataSource
    private lateinit var repository: WalletRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeDataSource = FakeWalletDataSource()
        repository = WalletRepositoryImpl(fakeDataSource)
    }

    // region getBillingBalance

    @Test
    fun `getBillingBalance maps DTO to domain model on success`() =
        runTest {
            fakeDataSource.billingBalanceResponse =
                BillingBalanceResponseDto(
                    success = true,
                    data = BillingBalanceDataDto(balancePaise = 50000L, balanceRupees = 500.0),
                )

            val result = repository.getBillingBalance("recipient-1")

            assertEquals(50000L, result.balancePaise)
            assertEquals(500.0, result.balanceRupees)
        }

    @Test
    fun `getBillingBalance throws YralException when data is null with error message`() =
        runTest {
            fakeDataSource.billingBalanceResponse =
                BillingBalanceResponseDto(
                    success = false,
                    data = null,
                    error = "Server error",
                )

            val exception =
                assertFailsWith<YralException> {
                    repository.getBillingBalance("recipient-1")
                }
            assertEquals("Server error", exception.text)
        }

    @Test
    fun `getBillingBalance throws YralException with fallback when error is also null`() =
        runTest {
            fakeDataSource.billingBalanceResponse =
                BillingBalanceResponseDto(
                    success = false,
                    data = null,
                    error = null,
                )

            val exception =
                assertFailsWith<YralException> {
                    repository.getBillingBalance("recipient-1")
                }
            assertEquals("Failed to fetch balance", exception.text)
        }

    // endregion

    // region getTransactions

    @Test
    fun `getTransactions maps DTOs to domain models correctly`() =
        runTest {
            fakeDataSource.transactionsResponse =
                BillingTransactionsResponseDto(
                    success = true,
                    data =
                        listOf(
                            TransactionResponseDto(
                                id = "tx-1",
                                userId = "user-principal-1",
                                transactionType = "SUBSCRIPTION",
                                amountPaise = 10000L,
                                recipientId = "recipient-1",
                                purchaseToken = "token-1",
                                createdAt = "2024-01-15T14:30:00Z",
                            ),
                        ),
                )

            val result = repository.getTransactions("recipient-1")

            assertEquals(1, result.size)
            val tx = result.first()
            assertEquals("tx-1", tx.id)
            assertEquals("user-principal-1", tx.userId)
            assertEquals("recipient-1", tx.recipientId)
            assertEquals("SUBSCRIPTION", tx.transactionType)
            assertEquals(10000L, tx.amountPaise)
            assertEquals("2024-01-15T14:30:00Z", tx.createdAt)
        }

    @Test
    fun `getTransactions always sets username to null`() =
        runTest {
            fakeDataSource.transactionsResponse =
                BillingTransactionsResponseDto(
                    success = true,
                    data =
                        listOf(
                            createTransactionDto(id = "tx-1"),
                            createTransactionDto(id = "tx-2"),
                        ),
                )

            val result = repository.getTransactions("recipient-1")

            assertTrue(
                result.all {
                    assertNull(it.username)
                    true
                },
            )
        }

    @Test
    fun `getTransactions throws YralException when data is null`() =
        runTest {
            fakeDataSource.transactionsResponse =
                BillingTransactionsResponseDto(
                    success = false,
                    data = null,
                    error = "Fetch failed",
                )

            val exception =
                assertFailsWith<YralException> {
                    repository.getTransactions("recipient-1")
                }
            assertEquals("Fetch failed", exception.text)
        }

    @Test
    fun `getTransactions returns empty list for empty data`() =
        runTest {
            fakeDataSource.transactionsResponse =
                BillingTransactionsResponseDto(
                    success = true,
                    data = emptyList(),
                )

            val result = repository.getTransactions("recipient-1")

            assertTrue(result.isEmpty())
        }

    @Test
    fun `getTransactions maps multiple transactions correctly`() =
        runTest {
            fakeDataSource.transactionsResponse =
                BillingTransactionsResponseDto(
                    success = true,
                    data =
                        listOf(
                            createTransactionDto(id = "tx-1", amountPaise = 5000L),
                            createTransactionDto(id = "tx-2", amountPaise = 15000L),
                            createTransactionDto(id = "tx-3", amountPaise = 25000L),
                        ),
                )

            val result = repository.getTransactions("recipient-1")

            assertEquals(3, result.size)
            assertEquals(5000L, result[0].amountPaise)
            assertEquals(15000L, result[1].amountPaise)
            assertEquals(25000L, result[2].amountPaise)
        }

    // endregion

    // region helpers

    private fun createTransactionDto(
        id: String = "tx-1",
        userId: String = "user-principal-1",
        transactionType: String = "SUBSCRIPTION",
        amountPaise: Long = 10000L,
        recipientId: String = "recipient-1",
        purchaseToken: String = "token-1",
        createdAt: String = "2024-01-15T14:30:00Z",
    ) = TransactionResponseDto(
        id = id,
        userId = userId,
        transactionType = transactionType,
        amountPaise = amountPaise,
        recipientId = recipientId,
        purchaseToken = purchaseToken,
        createdAt = createdAt,
    )

    // endregion
}

private class FakeWalletDataSource : WalletDataSource {
    var billingBalanceResponse: BillingBalanceResponseDto =
        BillingBalanceResponseDto(
            success = true,
            data = BillingBalanceDataDto(balancePaise = 0L, balanceRupees = 0.0),
        )
    var transactionsResponse: BillingTransactionsResponseDto =
        BillingTransactionsResponseDto(
            success = true,
            data = emptyList(),
        )

    override suspend fun getBillingBalance(recipientId: String): BillingBalanceResponseDto = billingBalanceResponse

    override suspend fun getTransactions(recipientId: String): BillingTransactionsResponseDto = transactionsResponse

    override suspend fun getBtcConversionRate(
        idToken: String,
        countryCode: String,
    ): BtcPriceResponseDto = throw NotImplementedError()

    override suspend fun getUserBtcBalance(
        canisterId: String,
        userPrincipal: String,
    ): String = throw NotImplementedError()

    override suspend fun getUserDolrBalance(
        canisterId: String,
        userPrincipal: String,
    ): String = throw NotImplementedError()

    override suspend fun getBtcRewardConfig(): BtcRewardConfigResponseDto = throw NotImplementedError()

    override suspend fun getDolrUsdPrice(): DolrPriceResponseDto = throw NotImplementedError()

    override suspend fun getBalance(userPrincipal: String): GetBalanceResponseDto = throw NotImplementedError()
}
