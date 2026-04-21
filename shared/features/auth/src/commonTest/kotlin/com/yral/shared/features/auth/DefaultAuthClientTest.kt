package com.yral.shared.features.auth

import com.russhwolf.settings.MapSettings
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.features.auth.domain.models.PhoneAuthLoginResponse
import com.yral.shared.features.auth.domain.models.PhoneAuthVerifyResponse
import com.yral.shared.features.auth.domain.models.TokenClaims
import com.yral.shared.features.auth.domain.models.TokenResponse
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ExchangePrincipalIdUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.PhoneAuthLoginUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.UpdateSessionAsRegisteredUseCase
import com.yral.shared.features.auth.domain.useCases.VerifyPhoneAuthUseCase
import com.yral.shared.features.auth.utils.OAuthResult
import com.yral.shared.features.auth.utils.OAuthUtils
import com.yral.shared.features.auth.utils.OAuthUtilsHelper
import com.yral.shared.features.auth.utils.SocialProvider
import com.yral.shared.features.wallet.domain.GetBalanceUseCase
import com.yral.shared.features.wallet.domain.models.BillingBalance
import com.yral.shared.features.wallet.domain.models.BtcRewardConfig
import com.yral.shared.features.wallet.domain.models.BtcToCurrency
import com.yral.shared.features.wallet.domain.models.DolrPrice
import com.yral.shared.features.wallet.domain.models.GetBalanceResponse
import com.yral.shared.features.wallet.domain.models.Transaction
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.stores.AccountDirectoryStore
import com.yral.shared.preferences.stores.AccountSessionPreferences
import com.yral.shared.preferences.stores.AffiliateAttributionStore
import com.yral.shared.preferences.stores.BotIdentitiesStore
import com.yral.shared.rust.service.utils.SignedDelegationPayload
import com.yral.shared.testsupport.preferences.FakePreferences
import com.yral.shared.testsupport.usecase.NoOpUseCaseFailureListener
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAuthClientTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cached bot cold start with valid id token skips refresh`() =
        runTest {
            val preferences = FakePreferences()
            val authRepository = RecordingAuthRepository()
            val helper = FakeOAuthUtilsHelper()
            val now = nowEpochSeconds()
            helper.claimsByToken[VALID_ID_TOKEN] = tokenClaims(expiry = now + VALID_TOKEN_OFFSET)
            storeCachedBotSession(preferences, idToken = VALID_ID_TOKEN, refreshToken = VALID_REFRESH_TOKEN)

            val sessionManager = SessionManager()
            val client = createClient(preferences, sessionManager, authRepository, helper)

            client.initialize()

            assertEquals(0, authRepository.refreshTokenCalls)
            assertEquals(BOT_PRINCIPAL, sessionManager.userPrincipal)
            assertTrue(sessionManager.isBotAccount == true)
            assertEquals(VALID_ID_TOKEN, preferences.getString(PrefKeys.ID_TOKEN.name))
        }

    @Test
    fun `cached bot cold start with expired id token refreshes with valid refresh token`() =
        runTest {
            val preferences = FakePreferences()
            val authRepository =
                RecordingAuthRepository(
                    refreshResponse =
                        TokenResponse(
                            idToken = REFRESHED_ID_TOKEN,
                            accessToken = REFRESHED_ACCESS_TOKEN,
                            expiresIn = VALID_TOKEN_OFFSET,
                            refreshToken = REFRESHED_REFRESH_TOKEN,
                            tokenType = "Bearer",
                        ),
                )
            val helper = FakeOAuthUtilsHelper()
            val now = nowEpochSeconds()
            helper.claimsByToken[EXPIRED_ID_TOKEN] = tokenClaims(expiry = now - EXPIRED_TOKEN_OFFSET)
            helper.claimsByToken[VALID_REFRESH_TOKEN] = tokenClaims(expiry = now + VALID_TOKEN_OFFSET)
            storeCachedBotSession(preferences, idToken = EXPIRED_ID_TOKEN, refreshToken = VALID_REFRESH_TOKEN)

            val sessionManager = SessionManager()
            val client = createClient(preferences, sessionManager, authRepository, helper)

            client.initialize()

            assertEquals(1, authRepository.refreshTokenCalls)
            assertEquals(VALID_REFRESH_TOKEN, authRepository.lastRefreshToken)
            assertEquals(BOT_PRINCIPAL, sessionManager.userPrincipal)
            assertTrue(sessionManager.isBotAccount == true)
            assertEquals(REFRESHED_ID_TOKEN, preferences.getString(PrefKeys.ID_TOKEN.name))
            assertEquals(REFRESHED_REFRESH_TOKEN, preferences.getString(PrefKeys.REFRESH_TOKEN.name))
            assertEquals(REFRESHED_ACCESS_TOKEN, preferences.getString(PrefKeys.ACCESS_TOKEN.name))
        }

    @Test
    fun `cached bot cold start with expired refresh token logs out`() =
        runTest {
            val preferences = FakePreferences()
            val authRepository = RecordingAuthRepository()
            val helper = FakeOAuthUtilsHelper()
            val now = nowEpochSeconds()
            helper.claimsByToken[EXPIRED_ID_TOKEN] = tokenClaims(expiry = now - EXPIRED_TOKEN_OFFSET)
            helper.claimsByToken[EXPIRED_REFRESH_TOKEN] = tokenClaims(expiry = now - EXPIRED_TOKEN_OFFSET)
            storeCachedBotSession(preferences, idToken = EXPIRED_ID_TOKEN, refreshToken = EXPIRED_REFRESH_TOKEN)

            val sessionManager = SessionManager()
            val client = createClient(preferences, sessionManager, authRepository, helper)

            client.initialize()

            assertEquals(0, authRepository.refreshTokenCalls)
            assertNull(sessionManager.userPrincipal)
            assertNull(preferences.getString(PrefKeys.ID_TOKEN.name))
            assertNull(preferences.getString(PrefKeys.REFRESH_TOKEN.name))
        }

    private fun TestScope.createClient(
        preferences: FakePreferences,
        sessionManager: SessionManager,
        authRepository: RecordingAuthRepository,
        oAuthUtilsHelper: FakeOAuthUtilsHelper,
    ): DefaultAuthClient {
        val dispatchers = AppDispatchers()
        val failureListener = NoOpUseCaseFailureListener()
        return DefaultAuthClient(
            sessionManager = sessionManager,
            analyticsManager = AnalyticsManager(),
            crashlyticsManager = CrashlyticsManager(),
            preferences = preferences,
            accountSessionPreferences = AccountSessionPreferences(preferences),
            accountDirectoryStore = AccountDirectoryStore(preferences),
            botIdentitiesStore = BotIdentitiesStore(preferences),
            authRepository = authRepository,
            requiredUseCases =
                DefaultAuthClient.RequiredUseCases(
                    authenticateTokenUseCase = AuthenticateTokenUseCase(dispatchers, failureListener, authRepository),
                    obtainAnonymousIdentityUseCase = ObtainAnonymousIdentityUseCase(dispatchers, failureListener, authRepository),
                    refreshTokenUseCase = RefreshTokenUseCase(dispatchers, failureListener, authRepository),
                    updateSessionAsRegisteredUseCase = UpdateSessionAsRegisteredUseCase(dispatchers, failureListener, authRepository),
                    exchangePrincipalIdUseCase = ExchangePrincipalIdUseCase(dispatchers, failureListener, authRepository),
                    getBalanceUseCase = GetBalanceUseCase(dispatchers, failureListener, FakeWalletRepository()),
                    registerNotificationTokenUseCase = RegisterNotificationTokenUseCase(dispatchers, failureListener, authRepository),
                    deregisterNotificationTokenUseCase = DeregisterNotificationTokenUseCase(dispatchers, failureListener, authRepository),
                    phoneAuthLoginUseCase = PhoneAuthLoginUseCase(dispatchers, failureListener, authRepository),
                    verifyPhoneAuthUseCase = VerifyPhoneAuthUseCase(dispatchers, failureListener, authRepository),
                ),
            oAuthUtils = NoOpOAuthUtils(),
            oAuthUtilsHelper = oAuthUtilsHelper,
            scope = backgroundScope,
            authTelemetry = AuthTelemetry(AnalyticsManager(), AffiliateAttributionStore(MapSettings())),
            initRustFactories = {},
            deregisterNotificationToken = {},
        )
    }

    private suspend fun storeCachedBotSession(
        preferences: FakePreferences,
        idToken: String,
        refreshToken: String,
    ) {
        preferences.putString(PrefKeys.MAIN_PRINCIPAL.name, MAIN_PRINCIPAL)
        preferences.putBytes(PrefKeys.MAIN_IDENTITY.name, MAIN_IDENTITY)
        preferences.putString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name, BOT_PRINCIPAL)
        preferences.putBytes(PrefKeys.IDENTITY.name, BOT_IDENTITY)
        preferences.putString(PrefKeys.CANISTER_ID.name, BOT_CANISTER)
        preferences.putString(PrefKeys.USER_PRINCIPAL.name, BOT_PRINCIPAL)
        preferences.putString(PrefKeys.PROFILE_PIC.name, BOT_PROFILE_PIC)
        preferences.putString(PrefKeys.USERNAME.name, BOT_USERNAME)
        preferences.putBoolean(PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name, true)
        preferences.putString(PrefKeys.ID_TOKEN.name, idToken)
        preferences.putString(PrefKeys.REFRESH_TOKEN.name, refreshToken)
    }

    private class RecordingAuthRepository(
        private val refreshResponse: TokenResponse =
            TokenResponse(
                idToken = "unused-id-token",
                accessToken = "unused-access-token",
                expiresIn = VALID_TOKEN_OFFSET,
                refreshToken = "unused-refresh-token",
                tokenType = "Bearer",
            ),
        private val refreshError: Throwable? = null,
    ) : AuthRepository {
        var refreshTokenCalls = 0
            private set
        var lastRefreshToken: String? = null
            private set
        var updateSessionAsRegisteredCalls = 0
            private set

        override suspend fun getOAuthUrl(
            provider: SocialProvider,
            identity: ByteArray,
        ): Pair<Url, String> = error("Not used in this test")

        override suspend fun obtainAnonymousIdentity(): TokenResponse = error("Not used in this test")

        override suspend fun authenticateToken(code: String): TokenResponse = error("Not used in this test")

        override suspend fun refreshToken(token: String): TokenResponse {
            refreshTokenCalls += 1
            lastRefreshToken = token
            refreshError?.let { throw it }
            return refreshResponse
        }

        override suspend fun updateSessionAsRegistered(
            idToken: String,
            canisterId: String,
            userPrincipal: String,
        ) {
            updateSessionAsRegisteredCalls += 1
        }

        override suspend fun exchangePrincipalId(
            idToken: String,
            principalId: String,
        ): ExchangePrincipalResponse = error("Not used in this test")

        override suspend fun deleteAccount(): String = error("Not used in this test")

        override suspend fun registerForNotifications(token: String) = error("Not used in this test")

        override suspend fun deregisterForNotifications(token: String) = error("Not used in this test")

        override suspend fun phoneAuthLogin(
            phoneNumber: String,
            identity: ByteArray,
        ): PhoneAuthLoginResponse = error("Not used in this test")

        override suspend fun verifyPhoneAuth(
            phoneNumber: String,
            code: String,
            clientState: String,
        ): PhoneAuthVerifyResponse = error("Not used in this test")

        override suspend fun createAiAccount(
            userPrincipal: String,
            signature: ByteArray,
            publicKey: ByteArray,
            signedMessage: ByteArray,
            ingressExpirySecs: Long,
            ingressExpiryNanos: Int,
            delegations: List<SignedDelegationPayload>?,
        ): ByteArray = error("Not used in this test")
    }

    private class FakeOAuthUtilsHelper : OAuthUtilsHelper {
        val claimsByToken = mutableMapOf<String, TokenClaims>()

        override fun generateCodeVerifier(): String = "verifier"

        override fun generateCodeChallenge(codeVerifier: String): String = "challenge"

        override fun generateState(): String = "state"

        override fun parseOAuthToken(token: String): TokenClaims = claimsByToken[token] ?: error("Unexpected token: $token")

        override fun mapUriToOAuthResult(uri: String): OAuthResult? = null
    }

    private class NoOpOAuthUtils : OAuthUtils {
        override var callBack: ((result: OAuthResult) -> Unit)? = null
        override var callbackExpiry: Long = 0L

        override fun openOAuth(
            context: Any,
            authUrl: Url,
            callBack: (result: OAuthResult) -> Unit,
        ) {
            this.callBack = callBack
        }

        override fun invokeCallback(result: OAuthResult) {
            callBack?.invoke(result)
        }

        override fun cleanup() {
            callBack = null
        }
    }

    private class FakeWalletRepository : WalletRepository {
        override suspend fun getBtcConversionRate(
            idToken: String,
            countryCode: String,
        ): BtcToCurrency = error("Not used in this test")

        override suspend fun getUserBtcBalance(
            canisterId: String,
            userPrincipal: String,
        ): String = error("Not used in this test")

        override suspend fun getUserDolrBalance(
            canisterId: String,
            userPrincipal: String,
        ): String = error("Not used in this test")

        override suspend fun getBtcRewardConfig(): BtcRewardConfig? = error("Not used in this test")

        override suspend fun getDolrPrice(): DolrPrice = error("Not used in this test")

        override suspend fun getBalance(userPrincipal: String): GetBalanceResponse = GetBalanceResponse(balance = 0)

        override suspend fun getBillingBalance(recipientId: String): BillingBalance = error("Not used in this test")

        override suspend fun getTransactions(recipientId: String): List<Transaction> = error("Not used in this test")
    }

    private companion object {
        private const val MAIN_PRINCIPAL = "main-principal"
        private const val BOT_PRINCIPAL = "bot-principal"
        private const val BOT_CANISTER = "bot-canister"
        private const val BOT_PROFILE_PIC = "https://example.com/bot.png"
        private const val BOT_USERNAME = "bot-user"
        private const val VALID_ID_TOKEN = "valid-id-token"
        private const val EXPIRED_ID_TOKEN = "expired-id-token"
        private const val VALID_REFRESH_TOKEN = "valid-refresh-token"
        private const val EXPIRED_REFRESH_TOKEN = "expired-refresh-token"
        private const val REFRESHED_ID_TOKEN = "refreshed-id-token"
        private const val REFRESHED_ACCESS_TOKEN = "refreshed-access-token"
        private const val REFRESHED_REFRESH_TOKEN = "refreshed-refresh-token"
        private const val VALID_TOKEN_OFFSET = 3_600L
        private const val EXPIRED_TOKEN_OFFSET = 60L
        private val MAIN_IDENTITY = byteArrayOf(1, 2, 3)
        private val BOT_IDENTITY = byteArrayOf(4, 5, 6)
    }
}

@OptIn(ExperimentalTime::class)
private fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

private fun tokenClaims(expiry: Long): TokenClaims =
    TokenClaims(
        aud = emptyList(),
        expiry = expiry,
        issuedAtTime = 0,
        issuerHost = "issuer",
        principal = "principal",
        nonce = null,
        extIsAnonymous = false,
        delegatedIdentity = null,
        email = null,
        botDelegatedIdentities = null,
    )
