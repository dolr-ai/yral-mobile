package com.yral.shared.features.wallet.data

import com.yral.shared.core.AppConfigurations.BILLING_BASE_URL
import com.yral.shared.core.AppConfigurations.OFF_CHAIN_BASE_URL
import com.yral.shared.core.AppConfigurations.PUMP_DUMP_BASE_URL
import com.yral.shared.data.removedFirebaseCloudFunctionsException
import com.yral.shared.features.wallet.data.models.BillingBalanceResponseDto
import com.yral.shared.features.wallet.data.models.BillingTransactionsResponseDto
import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto
import com.yral.shared.features.wallet.data.models.BtcRewardConfigResponseDto
import com.yral.shared.features.wallet.data.models.DolrPriceResponseDto
import com.yral.shared.features.wallet.data.models.GetBalanceResponseDto
import com.yral.shared.http.httpGet
import com.yral.shared.rust.service.domain.IndividualUserRepository
import io.ktor.client.HttpClient
import io.ktor.http.path
import kotlinx.serialization.json.Json

class WalletDataSourceImpl(
    private val httpClient: HttpClient,
    private val json: Json,
    private val individualUserRepository: IndividualUserRepository,
) : WalletDataSource {
    override suspend fun getBtcConversionRate(
        idToken: String,
        countryCode: String,
    ): BtcPriceResponseDto = throw removedFirebaseCloudFunctionsException("getBtcConversionRate")

    override suspend fun getUserBtcBalance(
        canisterId: String,
        userPrincipal: String,
    ): String =
        individualUserRepository
            .getUserBitcoinBalance(canisterId, userPrincipal)

    override suspend fun getUserDolrBalance(
        canisterId: String,
        userPrincipal: String,
    ): String =
        individualUserRepository
            .getUserDolrBalance(canisterId, userPrincipal)

    override suspend fun getBtcRewardConfig(): BtcRewardConfigResponseDto =
        httpGet(
            httpClient = httpClient,
            json = json,
            block = {
                url {
                    host = OFF_CHAIN_BASE_URL
                    path(BTC_REWARD_CONFIG_PATH)
                }
            },
        )

    override suspend fun getDolrUsdPrice(): DolrPriceResponseDto =
        httpGet(
            httpClient = httpClient,
            json = json,
            block = {
                url {
                    protocol = io.ktor.http.URLProtocol.HTTPS
                    host = COINGECKO_API_HOST
                    path(COINGECKO_PRICE_PATH)
                    parameters.append("ids", DOLR_COIN_ID)
                    parameters.append("vs_currencies", "usd,inr")
                }
            },
        )

    override suspend fun getBalance(userPrincipal: String): GetBalanceResponseDto =
        httpGet(
            httpClient,
            json,
        ) {
            url {
                host = PUMP_DUMP_BASE_URL
                path(GET_BALANCE_PATH, userPrincipal)
            }
        }

    override suspend fun getBillingBalance(recipientId: String): BillingBalanceResponseDto =
        httpGet(
            httpClient,
            json,
        ) {
            url {
                host = BILLING_BASE_URL
                path(BILLING_BALANCE_PATH)
                parameters.append("recipient_id", recipientId)
            }
        }

    override suspend fun getTransactions(recipientId: String): BillingTransactionsResponseDto =
        httpGet(
            httpClient,
            json,
        ) {
            url {
                host = BILLING_BASE_URL
                path(BILLING_TRANSACTIONS_PATH)
                parameters.append("recipient_id", recipientId)
            }
        }

    companion object {
        private const val GET_BALANCE_PATH = "v2/balance"
        private const val BTC_REWARD_CONFIG_PATH = "api/v1/rewards/config_v2"
        private const val COINGECKO_API_HOST = "api.coingecko.com"
        private const val COINGECKO_PRICE_PATH = "api/v3/simple/price"
        private const val DOLR_COIN_ID = "dolr-ai"
        private const val BILLING_BALANCE_PATH = "transactions/balance"
        private const val BILLING_TRANSACTIONS_PATH = "transactions"
    }
}
