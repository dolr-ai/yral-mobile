package com.yral.shared.features.wallet.data

import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.ktx.Firebase
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.wallet.data.models.BtcPriceResponseDto
import com.yral.shared.firebaseStore.cloudFunctionUrl
import com.yral.shared.rust.service.domain.IndividualUserRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

class WalletDataSourceImpl(
    private val httpClient: HttpClient,
    private val json: Json,
    private val individualUserRepository: IndividualUserRepository,
) : WalletDataSource {
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override suspend fun getBtcInInr(idToken: String): BtcPriceResponseDto {
        val response: HttpResponse =
            httpClient.get {
                expectSuccess = false
                url {
                    host = cloudFunctionUrl()
                    path(BTC_INR_VALUE_PATH)
                }
                val appCheckToken =
                    Firebase.appCheck
                        .getToken(false)
                        .await()
                        .token
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

    companion object {
        private const val BTC_INR_VALUE_PATH = "btc_inr_value"
        private const val HEADER_X_FIREBASE_APPCHECK = "X-Firebase-AppCheck"
    }
}
