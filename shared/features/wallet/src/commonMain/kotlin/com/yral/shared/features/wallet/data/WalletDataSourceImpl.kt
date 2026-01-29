package com.yral.shared.features.wallet.data

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto
import com.yral.shared.features.wallet.data.models.BtcRewardConfigResponseDto
import com.yral.shared.firebaseStore.cloudFunctionUrl
import com.yral.shared.firebaseStore.firebaseAppCheckToken
import com.yral.shared.http.httpGet
import com.yral.shared.rust.service.domain.IndividualUserRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import kotlinx.serialization.json.Json

class WalletDataSourceImpl(
    private val httpClient: HttpClient,
    private val json: Json,
    private val individualUserRepository: IndividualUserRepository,
) : WalletDataSource {
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getBtcConversionRate(
        idToken: String,
        countryCode: String,
    ): BtcPriceResponseDto {
        val response: HttpResponse =
            httpClient.get {
                expectSuccess = false
                url {
                    host = cloudFunctionUrl()
                    path(BTC_VALUE_BY_COUNTRY_PATH)
                    parameter("country_code", countryCode)
                }
                val appCheckToken = firebaseAppCheckToken()
                headers {
                    append(HttpHeaders.Authorization, "Bearer $idToken")
                    append(HEADER_X_FIREBASE_APPCHECK, appCheckToken)
                }
            }
        val apiResponseString = response.bodyAsText()
        if (response.status == HttpStatusCode.OK) {
            return json.decodeFromString(apiResponseString)
        } else {
            throw YralException(apiResponseString)
        }
    }

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
                    host = REWARDS_CONFIG_BASE_URL
                    path(BTC_REWARD_CONFIG_PATH)
                }
            },
        )

    companion object {
        private const val BTC_VALUE_BY_COUNTRY_PATH = "btc_value_by_country"
        private const val HEADER_X_FIREBASE_APPCHECK = "X-Firebase-AppCheck"
        private const val BTC_REWARD_CONFIG_PATH = "api/v1/rewards/config_v2"
        private const val REWARDS_CONFIG_BASE_URL = "pr-376-dolr-ai-off-chain-agent.fly.dev"
    }
}
