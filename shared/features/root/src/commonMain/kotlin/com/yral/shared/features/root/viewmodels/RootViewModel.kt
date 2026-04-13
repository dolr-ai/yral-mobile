package com.yral.shared.features.root.viewmodels

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.analytics.AnalyticsUtmParams
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.TokenType
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.AccountDirectory
import com.yral.shared.core.session.AccountDirectoryProfile
import com.yral.shared.core.session.ProDetails
import com.yral.shared.core.session.Session
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.core.session.hasSameUserPrincipal
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.data.domain.useCases.FetchDailyStreakUseCase
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.YralAuthException
import com.yral.shared.features.auth.YralFBAuthException
import com.yral.shared.features.root.analytics.RootTelemetry
import com.yral.shared.features.root.domain.DailyStreakLaunchEvaluator
import com.yral.shared.features.root.domain.DailyStreakLaunchResult
import com.yral.shared.features.subscriptions.domain.FetchProductsUseCase
import com.yral.shared.features.subscriptions.domain.QueryPurchaseUseCase
import com.yral.shared.iap.PurchaseResult
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.preferences.stores.AccountDirectoryStore
import com.yral.shared.preferences.stores.AccountSessionPreferences
import com.yral.shared.preferences.stores.BotIdentitiesStore
import com.yral.shared.preferences.stores.BotIdentityEntry
import com.yral.shared.preferences.stores.UtmAttributionStore
import com.yral.shared.preferences.stores.UtmParams
import com.yral.shared.rust.service.domain.models.SubscriptionPlan
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7Params
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7UseCase
import com.yral.shared.rust.service.services.HelperService
import com.yral.shared.rust.service.utils.authenticateWithNetwork
import com.yral.shared.rust.service.utils.getSessionFromIdentity
import com.yral.shared.rust.service.utils.propicFromPrincipal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

enum class RootError {
    TIMEOUT,
}

sealed interface NavigationTarget {
    data object Splash : NavigationTarget
    data object Home : NavigationTarget
    data object MandatoryLogin : NavigationTarget
}

@OptIn(ExperimentalTime::class, ExperimentalEncodingApi::class)
@Suppress("TooGenericExceptionCaught", "LongParameterList", "TooManyFunctions", "LargeClass")
class RootViewModel(
    private val appDispatchers: AppDispatchers,
    authClientFactory: AuthClientFactory,
    private val sessionManager: SessionManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val rootTelemetry: RootTelemetry,
    private val flagManager: FeatureFlagManager,
    private val preferences: Preferences,
    private val accountSessionPreferences: AccountSessionPreferences,
    private val accountDirectoryStore: AccountDirectoryStore,
    private val botIdentitiesStore: BotIdentitiesStore,
    private val utmAttributionStore: UtmAttributionStore,
    private val fetchDailyStreakUseCase: FetchDailyStreakUseCase,
    private val dailyStreakLaunchEvaluator: DailyStreakLaunchEvaluator,
    private val fetchProductsUseCase: FetchProductsUseCase,
    private val queryPurchaseUseCase: QueryPurchaseUseCase,
    private val getUserProfileDetailsV7UseCase: GetUserProfileDetailsV7UseCase,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.disk)
    private val json = Json { ignoreUnknownKeys = true }
    private var hasAttemptedAutoSwitch = false
    private val checkedDailyStreakPrincipals = mutableSetOf<String>()
    private val rootEventChannel = Channel<RootEvent>(Channel.BUFFERED)

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                // for async calls after setSession
                // updateSessionAsRegistered, FirebaseAuth/Coin balance update
                _state.update { it.copy(error = RootError.TIMEOUT) }
                Logger.e("Auth error - $e")
                crashlyticsManager.recordException(e, ExceptionType.AUTH)
            }

    internal var splashScreenTimeout: Long = SPLASH_SCREEN_TIMEOUT
    internal var initialDelayForSetup: Long = INITIAL_DELAY_FOR_SETUP

    companion object {
        const val SPLASH_SCREEN_TIMEOUT = 31000L // 31 seconds timeout
        const val INITIAL_DELAY_FOR_SETUP = 300L
        const val BOT_SOURCE_LOG_TAG = "BotIdentitySource"
        private const val ACCOUNT_DIALOG_RETRY_DELAY_MS = 500L
        private const val BOT_LOAD_MAX_ATTEMPTS = 3
        private const val BOT_LOAD_RETRY_DELAY_MS = 600L
        private const val JWT_PAYLOAD_INDEX = 1
        private const val BASE64_BLOCK_SIZE = 4
        private const val BASE64_PAD_CHAR = '='
    }

    private val _state = MutableStateFlow(RootState())
    val state: StateFlow<RootState> = _state.asStateFlow()
    val rootEvents = rootEventChannel.receiveAsFlow()

    private val analyticsUser =
        sessionManager.observeSessionStateWithProperty { state, properties ->
            sessionManager.userPrincipal?.let { userPrincipal ->
                sessionManager.canisterID?.let { canisterID ->
                    val isBotAccount = sessionManager.isBotAccount
                    User(
                        userId = userPrincipal,
                        canisterId = canisterID,
                        isLoggedIn = properties.isSocialSignIn,
                        isCreator = properties.profileVideosCount?.let { it > 0 },
                        walletBalance = properties.coinBalance?.toDouble(),
                        tokenType = TokenType.YRAL,
                        emailId = properties.emailId,
                        utmParams = utmAttributionStore.get()?.toAnalyticsUtmParams(),
                        isMandatoryLogin = properties.isMandatoryLogin,
                        phoneNumber = properties.phoneNumber,
                        proStatus = properties.proDetails?.isProPurchased,
                        isHonExperiment = null,
                        isBotAccount = isBotAccount,
                        parentAccount = if (isBotAccount == true) properties.accountDirectory?.mainPrincipal else null,
                    )
                }
            }
        }

    private var initialisationJob: Job? = null
    private var firebaseJob: Job? = null

    @OptIn(ExperimentalTime::class)
    private val splashStartTime = Clock.System.now()
    private var isFirstTimeUser: Boolean? = null

    init {
        coroutineScope.launch {
            sessionManager
                .observeSessionState(transform = { it })
                .collect { sessionState ->
                    if (sessionState != _state.value.sessionState) {
                        // session may be updated with user profile details while principal remains same
                        val isSessionPrincipalSame = sessionState.hasSameUserPrincipal(_state.value.sessionState)
                        _state.update {
                            it.copy(
                                sessionState = sessionState,
                                isPendingLogin =
                                    if (isSessionPrincipalSame) {
                                        it.isPendingLogin
                                    } else {
                                        UiState.InProgress()
                                    },
                            )
                        }
                        when (sessionState) {
                            is SessionState.Initial -> initialize()
                            is SessionState.SignedIn -> initialize(isSessionPrincipalSame)
                            else -> Unit
                        }
                    }
                }
        }
        coroutineScope.launch {
            analyticsUser.collect { user -> rootTelemetry.setUser(user) }
        }
        coroutineScope.launch {
            val isFirst = preferences.getString(PrefKeys.FIRST_APP_OPEN_DATE_TIME.name) == null
            isFirstTimeUser = isFirst
            if (isFirst) {
                val now = Clock.System.now()
                rootTelemetry.onFirstAppLaunch(now)
                preferences.putString(PrefKeys.FIRST_APP_OPEN_DATE_TIME.name, now.toString())
            }
        }
        coroutineScope.launch {
            fetchYralProAvailability()
        }
    }

    fun initialize(isSessionPrincipalSame: Boolean = false) {
        initialisationJob?.cancel()
        initialisationJob =
            coroutineScope.launch {
                _state.update { it.copy(error = null) }
                try {
                    withTimeout(splashScreenTimeout) {
                        checkLoginAndInitialize(isSessionPrincipalSame)
                    }
                } catch (e: TimeoutCancellationException) {
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Splash timeout - ${e.message}")
                    crashlyticsManager.recordException(
                        YralException("Splash screen timeout - initialization took too long"),
                        ExceptionType.AUTH,
                    )
                    throw e
                } catch (e: YralAuthException) {
                    // for async calls before setSession
                    _state.update { it.copy(error = RootError.TIMEOUT) }
                    Logger.e("Auth error - $e")
                    crashlyticsManager.recordException(e, ExceptionType.AUTH)
                }
            }
    }

    fun onHomeReached() {
        val userPrincipal = sessionManager.userPrincipal ?: return
        if (!checkedDailyStreakPrincipals.add(userPrincipal)) return

        coroutineScope.launch {
            fetchDailyStreakUseCase
                .invoke(FetchDailyStreakUseCase.Params(userPrincipal = userPrincipal))
                .onSuccess { streak ->
                    when (
                        val result =
                            dailyStreakLaunchEvaluator.evaluate(
                                principal = userPrincipal,
                                remoteStreakCount = streak.streakCount,
                            )
                    ) {
                        is DailyStreakLaunchResult.NoChange -> {
                            Unit
                        }

                        is DailyStreakLaunchResult.ShowCelebration -> {
                            rootEventChannel.send(
                                RootEvent.ShowDailyStreakCelebration(streakCount = result.streakCount),
                            )
                        }
                    }
                }.onFailure { error ->
                    Logger.w("RootViewModel") {
                        "Failed to fetch daily streak on home entry: ${error.message}"
                    }
                }
        }
    }

    private suspend fun checkLoginAndInitialize(isSessionPrincipalSame: Boolean) {
        delay(initialDelayForSetup)
        sessionManager.identity?.let {
            sessionManager.updateSocialSignInStatus(
                isSocialSignIn = preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false,
            )
            if (!isSessionPrincipalSame) {
                resolveNavigationTarget()
                initializeFirebase()
                // Not used as of now we will get details from canister in profileDetailsV6
                // restorePurchases()
            }
            if (_state.value.accountDialogInfo == null) {
                restoreAccountDialogFromCache()?.let { cachedInfo ->
                    _state.update { current ->
                        current.copy(accountDialogInfo = cachedInfo)
                    }
                } ?: seedAccountDialogFromLocalData()?.let { localInfo ->
                    _state.update { current ->
                        current.copy(accountDialogInfo = localInfo)
                    }
                }
            }
            if (!_state.value.isAccountSwitchInProgress) {
                refreshAccountDirectoryInBackground()
            }
            autoSwitchToLastActiveAccount()
        } ?: authClient.initialize()
    }

    private suspend fun initializeFirebase() {
        val isFirebaseAuthenticated =
            sessionManager.readLatestSessionPropertyWithDefault(
                selector = { it.isFirebaseLoggedIn },
                defaultValue = false,
            )
        if (isFirebaseAuthenticated || isPendingLogin()) return
        firebaseJob?.cancel()
        val session = (_state.value.sessionState as? SessionState.SignedIn)?.session ?: return
        firebaseJob =
            coroutineScope.launch {
                try {
                    authClient.fetchBalance(session)
                    authClient.authorizeFirebase()
                } catch (e: YralFBAuthException) {
                    // Do not update error in state since no error message required
                    Logger.e("Firebase Auth error - $e")
                    crashlyticsManager.recordException(e, ExceptionType.AUTH)
                } catch (e: YralAuthException) {
                    // can be triggered in postFirebaseLogin when getting balance
                    Logger.e("Fetch Balance error - $e")
                    crashlyticsManager.recordException(e, ExceptionType.AUTH)
                }
            }
    }

    private suspend fun fetchYralProAvailability() {
        fetchProductsUseCase(listOf(ProductId.YRAL_PRO))
            .onSuccess { products ->
                val isAvailable = products.any { it.id == ProductId.YRAL_PRO.productId }
                sessionManager.updateYralProAvailability(isAvailable)
                Logger.d("SubscriptionX") { "YRAL Pro available: $isAvailable" }
            }.onFailure { error ->
                Logger.e("SubscriptionX", error) { "Failed to fetch IAP products for availability check" }
            }
    }

    @Suppress("UnusedPrivateMember")
    private fun restorePurchases() {
        coroutineScope.launch {
            val isSocialSignIn =
                sessionManager.readLatestSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                )
            if (!isSocialSignIn) return@launch
            queryPurchaseUseCase(ProductId.YRAL_PRO)
                .onSuccess { isPurchased -> Logger.d("SubscriptionX") { "isPurchased: $isPurchased" } }
                .onFailure { Logger.e("SubscriptionX", it) { "Failed to restore" } }
        }
    }

    private suspend fun resolveNavigationTarget() {
        // Wait for remote config and check MandatoryLogin flag
        val isSocialSignedIn = preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name) ?: false
        if (preferences.getBoolean(PrefKeys.IS_REMOTE_CONFIG_FORCE_SYNCED.name) != true) {
            val fetched = flagManager.awaitRemoteFetch(5.seconds)
            if (fetched) {
                preferences.putBoolean(PrefKeys.IS_REMOTE_CONFIG_FORCE_SYNCED.name, true)
            }
        }
        val isMandatoryLoginEnabled = flagManager.isEnabled(AppFeatureFlags.Common.MandatoryLogin)
        sessionManager.updateIsMandatoryLogin(isMandatoryLoginEnabled)
        val navigationTarget =
            if (isMandatoryLoginEnabled) {
                if (!isSocialSignedIn) {
                    NavigationTarget.MandatoryLogin to true
                } else {
                    NavigationTarget.Home to false
                }
            } else {
                NavigationTarget.Home to false
            }
        if (navigationTarget.first == NavigationTarget.Home) {
            trackSplashDurationForFirstUser()
        }
        _state.update {
            it.copy(
                navigationTarget = navigationTarget.first,
                isLoginMandatory = navigationTarget.second,
                isPendingLogin = UiState.Success(navigationTarget.second && !isSocialSignedIn),
            )
        }
    }

    fun onSplashAnimationComplete() {
        _state.update { it.copy(initialAnimationComplete = true) }
    }

    fun updateProfileVideosCount(count: Int) {
        sessionManager.updateProfileVideosCount(count)
    }

    fun splashScreenViewed() {
        rootTelemetry.onSplashScreenViewed()
    }

    @OptIn(ExperimentalTime::class)
    private fun trackSplashDurationForFirstUser() {
        if (isFirstTimeUser != true) return
        val durationMs = (Clock.System.now() - splashStartTime).inWholeMilliseconds
        rootTelemetry.onSplashScreenDuration(durationMs)
    }

    fun bottomNavigationClicked(categoryName: CategoryName) {
        rootTelemetry.bottomNavigationClicked(categoryName)
    }

    private fun UtmParams.toAnalyticsUtmParams(): AnalyticsUtmParams =
        AnalyticsUtmParams(
            source = source,
            medium = medium,
            campaign = campaign,
            term = term,
            content = content,
            gclid = gclid,
        )

    private fun refreshAccountDirectoryInBackground() {
        coroutineScope.launch {
            refreshAccountDirectory(allowRetry = true)
        }
    }

    private suspend fun refreshAccountDirectory(allowRetry: Boolean) {
        val activePrincipal = sessionManager.userPrincipal
        val mainPrincipal =
            accountSessionPreferences.getMainPrincipal()
                ?: preferences.getString(PrefKeys.USER_PRINCIPAL.name)
        val mainIdentity =
            accountSessionPreferences.getMainIdentity()
                ?: preferences
                    .getBytes(PrefKeys.IDENTITY.name)
                    ?.takeIf { activePrincipal == mainPrincipal }
        val botEntries = loadBotEntries().filter { it.principal != mainPrincipal }
        val (mainAccount, botAccounts) =
            resolveAllAccounts(
                activePrincipal = activePrincipal,
                mainPrincipal = mainPrincipal,
                mainIdentity = mainIdentity,
                botEntries = botEntries,
            )

        if (mainAccount != null || botAccounts.isNotEmpty()) {
            val directory =
                buildAccountDirectory(
                    mainPrincipal = mainPrincipal,
                    mainAccount = mainAccount,
                    botAccounts = botAccounts,
                )
            applyAccountDirectory(directory)
        } else if (allowRetry) {
            // ID token may not be persisted yet right after login; retry once shortly.
            delay(ACCOUNT_DIALOG_RETRY_DELAY_MS)
            refreshAccountDirectory(allowRetry = false)
        } else {
            Logger.d("RootViewModel") {
                "refreshAccountDirectory: no accounts resolved, keeping cached directory"
            }
        }
    }

    private suspend fun resolveAllAccounts(
        activePrincipal: String?,
        mainPrincipal: String?,
        mainIdentity: ByteArray?,
        botEntries: List<BotIdentityEntry>,
    ): Pair<AccountUi?, List<AccountUi>> {
        val mainFallbackUsername = sessionManager.username
        return coroutineScope {
            val mainDeferred =
                async {
                    resolveAccountUi(
                        principal = mainPrincipal,
                        identityBytes = mainIdentity,
                        isBot = false,
                        activePrincipal = activePrincipal,
                        fallbackUsername = mainFallbackUsername,
                    )
                }
            val botDeferred =
                botEntries.map { entry ->
                    async {
                        resolveAccountUi(
                            principal = entry.principal,
                            identityBytes = decodeBotIdentity(entry),
                            isBot = true,
                            activePrincipal = activePrincipal,
                            fallbackUsername = entry.username,
                        )
                    }
                }
            mainDeferred.await() to botDeferred.awaitAll().filterNotNull()
        }
    }

    private fun decodeBotIdentity(entry: BotIdentityEntry): ByteArray? =
        runCatching { Base64.decode(entry.identity) }
            .onFailure {
                crashlyticsManager.recordException(
                    YralException("Base64 decode failed for bot ${entry.principal}", it),
                )
                Logger.d("RootViewModel") {
                    "Base64 decode failed for bot ${entry.principal}: ${it.message}"
                }
            }.getOrNull()

    private fun buildAccountDirectory(
        mainPrincipal: String?,
        mainAccount: AccountUi?,
        botAccounts: List<AccountUi>,
    ): AccountDirectory {
        val profiles =
            buildMap {
                mainAccount?.let {
                    put(
                        it.principal,
                        AccountDirectoryProfile(
                            principal = it.principal,
                            username = it.name,
                            avatarUrl = it.avatarUrl,
                            isBot = false,
                        ),
                    )
                }
                botAccounts.forEach { bot ->
                    put(
                        bot.principal,
                        AccountDirectoryProfile(
                            principal = bot.principal,
                            username = bot.name,
                            avatarUrl = bot.avatarUrl,
                            isBot = true,
                        ),
                    )
                }
            }
        return AccountDirectory(
            mainPrincipal = mainPrincipal,
            botPrincipals = botAccounts.map { it.principal },
            profilesByPrincipal = profiles,
        )
    }

    private suspend fun resolveAccountUi(
        principal: String?,
        identityBytes: ByteArray?,
        isBot: Boolean,
        activePrincipal: String?,
        fallbackUsername: String?,
    ): AccountUi? {
        if (principal == null) return null
        val details =
            runCatching {
                identityBytes?.let { authenticateWithNetwork(it) }
            }.onFailure {
                crashlyticsManager.recordException(
                    YralException("authenticateWithNetwork failed for $principal", it),
                )
                Logger.d("RootViewModel") {
                    "authenticateWithNetwork failed for $principal: ${it.message}"
                }
            }.getOrNull()
        val resolvedUsername =
            resolveUsername(details?.username, principal)
                ?: fallbackUsername?.takeUnless { username -> username.isBlank() }
                ?: principal
        return AccountUi(
            principal = principal,
            name = resolvedUsername,
            avatarUrl = details?.profilePic ?: propicFromPrincipal(principal),
            isBot = isBot,
            isActive = principal == activePrincipal,
        )
    }

    private suspend fun applyAccountDirectory(directory: AccountDirectory) {
        val activePrincipal = sessionManager.userPrincipal
        sessionManager.updateAccountDirectory(directory)
        persistAccountDirectoryCache(directory)
        val accountDialogInfo = toAccountDialogInfo(directory, activePrincipal)
        _state.update { current ->
            current.copy(accountDialogInfo = accountDialogInfo)
        }
    }

    private fun toAccountDialogInfo(
        directory: AccountDirectory,
        activePrincipal: String?,
    ): AccountDialogInfo {
        val mainAccount =
            directory.mainPrincipal
                ?.let { directory.profilesByPrincipal[it] }
                ?.toAccountUi(activePrincipal)
        val botAccounts =
            directory.botPrincipals
                .mapNotNull { principal -> directory.profilesByPrincipal[principal]?.toAccountUi(activePrincipal) }
        return AccountDialogInfo(mainAccount = mainAccount, botAccounts = botAccounts)
    }

    private suspend fun restoreAccountDialogFromCache(): AccountDialogInfo? =
        accountDirectoryStore
            .get()
            ?.let { directory ->
                sessionManager.updateAccountDirectory(directory)
                toAccountDialogInfo(directory, sessionManager.userPrincipal)
            }

    private suspend fun seedAccountDialogFromLocalData(): AccountDialogInfo? {
        val activePrincipal = sessionManager.userPrincipal
        val mainPrincipal =
            accountSessionPreferences.getMainPrincipal()
                ?: preferences.getString(PrefKeys.USER_PRINCIPAL.name)
                ?: return null
        val botEntries =
            botIdentitiesStore
                .get()
                .filter { it.principal != mainPrincipal }

        val mainUsername =
            if (activePrincipal == mainPrincipal) {
                resolveUsername(sessionManager.username, mainPrincipal)
            } else {
                mainPrincipal
            } ?: mainPrincipal
        val mainAvatar =
            if (activePrincipal == mainPrincipal) {
                sessionManager.profilePic ?: propicFromPrincipal(mainPrincipal)
            } else {
                propicFromPrincipal(mainPrincipal)
            }
        val mainAccount =
            AccountUi(
                principal = mainPrincipal,
                name = mainUsername,
                avatarUrl = mainAvatar,
                isBot = false,
                isActive = mainPrincipal == activePrincipal,
            )
        val botAccounts =
            botEntries.map { entry ->
                AccountUi(
                    principal = entry.principal,
                    name = resolveUsername(entry.username, entry.principal) ?: entry.principal,
                    avatarUrl = propicFromPrincipal(entry.principal),
                    isBot = true,
                    isActive = entry.principal == activePrincipal,
                )
            }
        val directory =
            buildAccountDirectory(
                mainPrincipal = mainPrincipal,
                mainAccount = mainAccount,
                botAccounts = botAccounts,
            )
        applyAccountDirectory(directory)
        return AccountDialogInfo(mainAccount = mainAccount, botAccounts = botAccounts)
    }

    private suspend fun persistAccountDirectoryCache(directory: AccountDirectory) {
        accountDirectoryStore.put(directory)
    }

    fun dismissAccountDialog() {
        _state.update { it.copy(showAccountDialog = false) }
    }

    @Suppress("LongMethod")
    fun switchToAccount(
        principal: String,
        onComplete: (Boolean) -> Unit = {},
    ) {
        if (_state.value.isAccountSwitchInProgress) {
            coroutineScope.launch(appDispatchers.main) { onComplete(false) }
            return
        }
        Logger.d("BotDeleteFlow") { "switchToAccount requested principal=$principal" }
        _state.update {
            it.copy(
                showAccountDialog = false,
                isAccountSwitchInProgress = true,
            )
        }
        coroutineScope.launch {
            val previousSessionState = _state.value.sessionState
            var switched = false
            runCatching {
                val current = sessionManager.userPrincipal
                if (current == principal) {
                    Logger.d("BotDeleteFlow") { "switchToAccount no-op principal already active=$principal" }
                    _state.update {
                        it.copy(
                            showAccountDialog = false,
                            isAccountSwitchInProgress = false,
                        )
                    }
                    switched = true
                    return@runCatching
                }
                sessionManager.updateState(SessionState.Loading)
                val identityBytes: ByteArray
                val isBot: Boolean
                val botUsername: String?
                val storedMainPrincipal = accountSessionPreferences.getMainPrincipal()
                if (principal == storedMainPrincipal) {
                    identityBytes =
                        accountSessionPreferences.getMainIdentity()
                            ?: throw YralException("Main identity missing")
                    isBot = false
                    botUsername = null
                } else {
                    val storedBots = botIdentitiesStore.get()
                    val match =
                        storedBots.firstOrNull { it.principal == principal }
                            ?: throw YralException("Bot identity not found")
                    identityBytes = Base64.decode(match.identity)
                    isBot = true
                    botUsername = match.username
                }

                val canisterData = authenticateWithNetwork(identityBytes)
                val resolvedUsername =
                    resolveUsername(canisterData.username ?: botUsername, principal)
                HelperService.initServiceFactories(identityBytes)
                val session =
                    Session(
                        identity = identityBytes,
                        canisterId = canisterData.canisterId,
                        userPrincipal = canisterData.userPrincipalId,
                        profilePic = canisterData.profilePic,
                        username = resolvedUsername,
                        bio = null,
                        isCreatedFromServiceCanister = canisterData.isCreatedFromServiceCanister,
                        isBotAccount = isBot,
                    )
                sessionManager.updateState(SessionState.SignedIn(session = session))
                cacheSession(identityBytes, session)
                accountSessionPreferences.setLastActivePrincipal(principal)
                // Refresh tokens and notification registration similar to post-login
                if (isBot) {
                    // Skip auth initialization for bots to avoid overwriting the active bot session with parent tokens
                    sessionManager.updateFirebaseLoginState(false)
                    authClient.fetchBalance(session)
                } else {
                    authClient.initialize()
                    authClient.authorizeFirebase()
                    authClient.fetchBalance(session)
                }
                updateAccountDialogForSwitchedProfile(
                    principal = principal,
                    isBot = isBot,
                    username = resolvedUsername ?: principal,
                    avatarUrl = canisterData.profilePic ?: propicFromPrincipal(principal),
                )
                switched = true
                Logger.d("BotDeleteFlow") {
                    "switchToAccount success principal=$principal isBot=$isBot"
                }
            }.onFailure { error ->
                Logger.e("BotDeleteFlow", error) {
                    "switchToAccount failed principal=$principal restoringPreviousState"
                }
                Logger.e("RootViewModel") { "Failed to switch account: ${error.message}" }
                sessionManager.updateState(previousSessionState)
            }.also {
                _state.update { current -> current.copy(isAccountSwitchInProgress = false) }
                coroutineScope.launch(appDispatchers.main) { onComplete(switched) }
            }
        }
    }

    fun showAccountSwitcher() {
        if (_state.value.isAccountSwitchInProgress) return
        if (_state.value.sessionState is SessionState.Loading) return
        coroutineScope.launch {
            if (_state.value.accountDialogInfo == null) {
                val cachedOrSeeded = restoreAccountDialogFromCache() ?: seedAccountDialogFromLocalData()
                if (cachedOrSeeded != null) {
                    _state.update { current -> current.copy(accountDialogInfo = cachedOrSeeded) }
                }
            }
            _state.update { it.copy(showAccountDialog = true) }
        }
    }

    fun switchToMainAccount(onComplete: (Boolean) -> Unit = {}) {
        coroutineScope.launch {
            val mainPrincipal =
                accountSessionPreferences.getMainPrincipal()
                    ?: preferences.getString(PrefKeys.USER_PRINCIPAL.name)
            if (mainPrincipal == null) {
                Logger.w("BotDeleteFlow") { "switchToMainAccount skipped: main principal missing" }
                onComplete(false)
                return@launch
            }
            accountSessionPreferences.setLastActivePrincipal(mainPrincipal)
            Logger.d("BotDeleteFlow") { "switchToMainAccount mainPrincipal=$mainPrincipal" }
            switchToAccount(mainPrincipal) { switched ->
                Logger.d("BotDeleteFlow") {
                    "switchToMainAccount completion mainPrincipal=$mainPrincipal switched=$switched"
                }
                onComplete(switched)
            }
        }
    }

    private suspend fun updateAccountDialogForSwitchedProfile(
        principal: String,
        isBot: Boolean,
        username: String,
        avatarUrl: String,
    ) {
        val current =
            sessionManager.accountDirectory
                ?: try {
                    restoreAccountDialogFromCache()
                    sessionManager.accountDirectory
                } catch (_: Throwable) {
                    null
                }
        val updatedDirectory =
            if (current != null) {
                val updatedBots =
                    if (isBot) {
                        (current.botPrincipals + principal).distinct()
                    } else {
                        current.botPrincipals
                    }
                current.copy(
                    mainPrincipal = if (isBot) current.mainPrincipal else principal,
                    botPrincipals = updatedBots,
                    profilesByPrincipal =
                        current.profilesByPrincipal + (
                            principal to
                                AccountDirectoryProfile(
                                    principal = principal,
                                    username = username,
                                    avatarUrl = avatarUrl,
                                    isBot = isBot,
                                )
                        ),
                )
            } else {
                AccountDirectory(
                    mainPrincipal = if (isBot) accountSessionPreferences.getMainPrincipal() else principal,
                    botPrincipals = if (isBot) listOf(principal) else emptyList(),
                    profilesByPrincipal =
                        mapOf(
                            principal to
                                AccountDirectoryProfile(
                                    principal = principal,
                                    username = username,
                                    avatarUrl = avatarUrl,
                                    isBot = isBot,
                                ),
                        ),
                )
            }
        applyAccountDirectory(updatedDirectory)
    }

    private suspend fun cacheSession(
        identity: ByteArray,
        session: Session,
    ) {
        preferences.putBytes(PrefKeys.IDENTITY.name, identity)
        session.canisterId?.let { preferences.putString(PrefKeys.CANISTER_ID.name, it) }
        session.userPrincipal?.let { preferences.putString(PrefKeys.USER_PRINCIPAL.name, it) }
        session.profilePic?.let { preferences.putString(PrefKeys.PROFILE_PIC.name, it) }
        val resolvedUsername =
            resolveUsername(session.username, session.userPrincipal)
        if (resolvedUsername != null) {
            preferences.putString(PrefKeys.USERNAME.name, resolvedUsername)
        } else {
            preferences.remove(PrefKeys.USERNAME.name)
        }
        preferences.putBoolean(
            PrefKeys.IS_CREATED_FROM_SERVICE_CANISTER.name,
            session.isCreatedFromServiceCanister,
        )
        if (!session.isBotAccount) {
            accountSessionPreferences.setMainIdentity(identity)
            accountSessionPreferences.setMainPrincipal(session.userPrincipal)
        }
    }

    private suspend fun autoSwitchToLastActiveAccount() {
        if (!hasAttemptedAutoSwitch) {
            hasAttemptedAutoSwitch = true
            val targetPrincipal = accountSessionPreferences.getLastActivePrincipal()
            val currentPrincipal = sessionManager.userPrincipal
            if (
                targetPrincipal != null &&
                currentPrincipal != null &&
                targetPrincipal != currentPrincipal
            ) {
                val mainPrincipal =
                    accountSessionPreferences.getMainPrincipal()
                        ?: preferences.getString(PrefKeys.USER_PRINCIPAL.name)

                val shouldSwitchToMain = targetPrincipal == mainPrincipal
                val shouldSwitchToBot =
                    !shouldSwitchToMain &&
                        loadBotEntries()
                            .any { it.principal == targetPrincipal }

                if (shouldSwitchToMain || shouldSwitchToBot) {
                    switchToAccount(targetPrincipal)
                }
            }
        }
    }

    fun isPendingLogin(): Boolean =
        with(_state.value) {
            isPendingLogin !is UiState.Success || isPendingLogin.data
        }

    fun checkSubscriptionAndOpen(
        openSubscription: (purchaseTimeMs: Long?) -> Unit,
        showSubscriptionAccountMismatchSheet: () -> Unit,
        onError: (() -> Unit)? = null,
    ) {
        coroutineScope.launch {
            if (!flagManager.isEnabled(AppFeatureFlags.Common.EnableSubscription)) return@launch
            val userPrincipal = sessionManager.userPrincipal
            if (userPrincipal == null) {
                withContext(appDispatchers.main) { onError?.invoke() }
                return@launch
            }
            val result = queryPurchaseUseCase(ProductId.YRAL_PRO)
            result
                .onSuccess { purchaseResult ->
                    withContext(appDispatchers.main) {
                        when (purchaseResult) {
                            is PurchaseResult.NoPurchase -> {
                                openSubscription(null)
                            }

                            is PurchaseResult.PurchaseMatches -> {
                                openSubscription(purchaseResult.purchaseTime)
                            }

                            is PurchaseResult.AccountMismatch -> {
                                showSubscriptionAccountMismatchSheet()
                            }

                            is PurchaseResult.UnaccountedPurchase -> {
                                Logger.d("SubscriptionX") { "Unaccounted purchase" }
                                onError?.invoke()
                            }
                        }
                    }
                }.onFailure {
                    Logger.d("SubscriptionX") { "Failed to query purchase $it" }
                    withContext(appDispatchers.main) { onError?.invoke() }
                }
        }
    }

    fun refreshCreditBalances() {
        coroutineScope.launch {
            val principal = sessionManager.userPrincipal ?: return@launch
            getUserProfileDetailsV7UseCase(
                GetUserProfileDetailsV7Params(
                    principal = principal,
                    targetPrincipal = principal,
                ),
            ).onSuccess { details ->
                val proPlan = details.subscriptionPlan as? SubscriptionPlan.Pro
                proPlan?.let {
                    sessionManager.updateProDetails(
                        details =
                            ProDetails(
                                isProPurchased = true,
                                availableCredits = proPlan.subscription.freeVideoCreditsLeft.toInt(),
                                totalCredits = proPlan.subscription.totalVideoCreditsAlloted.toInt(),
                            ),
                    )
                }
                Logger.d("SubscriptionX") { "Updated pro details $proPlan" }
            }.onFailure {
                Logger.e("SubscriptionX", it) { "Failed to update pro details" }
            }
        }
    }
    private suspend fun loadBotEntries(): List<BotIdentityEntry> {
        var result: List<BotIdentityEntry>? = null
        repeat(BOT_LOAD_MAX_ATTEMPTS) { attempt ->
            if (result != null) return@repeat

            val cachedBots = botIdentitiesStore.get()
            if (cachedBots.isNotEmpty()) {
                Logger.d("RootViewModel") { "loadBotEntries: using cached ${cachedBots.size} bots" }
                Logger.d(BOT_SOURCE_LOG_TAG) {
                    "loadBotEntries source=local_pref count=${cachedBots.size}"
                }
                sessionManager.updateBotCount(cachedBots.size)
                result = cachedBots
                return@repeat
            }

            val tokenBots =
                preferences
                    .getString(PrefKeys.ID_TOKEN.name)
                    ?.let(::parseBotsFromToken)
                    .orEmpty()
            if (tokenBots.isNotEmpty()) {
                botIdentitiesStore.put(tokenBots)
                sessionManager.updateBotCount(tokenBots.size)
                result = tokenBots
                return@repeat
            }

            if (attempt < BOT_LOAD_MAX_ATTEMPTS - 1) {
                Logger.d("RootViewModel") { "loadBotEntries: empty on attempt $attempt, retrying..." }
                Logger.d(BOT_SOURCE_LOG_TAG) {
                    "loadBotEntries source=none count=0 attempt=$attempt retry=true"
                }
                delay(BOT_LOAD_RETRY_DELAY_MS)
            }
        }
        if (result == null) {
            Logger.d("RootViewModel") { "loadBotEntries: no bots found after retries" }
            Logger.d(BOT_SOURCE_LOG_TAG) {
                "loadBotEntries source=none count=0 attempts=$BOT_LOAD_MAX_ATTEMPTS"
            }
            sessionManager.updateBotCount(0)
        }
        return result ?: emptyList()
    }

    private fun parseBotsFromToken(idToken: String): List<BotIdentityEntry> {
        val payloadJson = decodeJwtPayload(idToken)
        val payloadElement =
            payloadJson?.let {
                runCatching<JsonElement> {
                    json.parseToJsonElement(it)
                }.getOrNull()
            }
        val botArray = payloadElement?.jsonObject?.get("ext_ai_account_delegated_identities")
        return if (payloadJson == null || botArray == null) {
            if (payloadElement != null && botArray == null) {
                Logger.d("RootViewModel") {
                    "parseBotsFromToken: no ext_ai_account_delegated_identities. " +
                        "Payload keys=${payloadElement.jsonObject.keys}"
                }
            }
            emptyList()
        } else {
            val rawStrings: List<String> =
                botArray.jsonArray.mapNotNull { element ->
                    when {
                        element is kotlinx.serialization.json.JsonPrimitive && element.isString -> {
                            element.content
                        }

                        element is kotlinx.serialization.json.JsonPrimitive && element.isString.not() -> {
                            element.content
                        }

                        else -> {
                            element.toString()
                        }
                    }
                }

            rawStrings.mapNotNull { raw ->
                runCatching {
                    val identityBytes =
                        decodeBase64Flexible(raw)
                            ?: raw.encodeToByteArray()
                    val principal = getSessionFromIdentity(identityBytes).userPrincipalId
                    BotIdentityEntry(principal = principal, identity = Base64.encode(identityBytes))
                }.onFailure { error ->
                    Logger.e("RootViewModel") {
                        "parseBotsFromToken: failed to decode one entry: ${error.message}"
                    }
                }.getOrNull()
            }
        }
    }

    private fun decodeJwtPayload(idToken: String): String? {
        val payloadPart = idToken.split(".").getOrNull(JWT_PAYLOAD_INDEX) ?: return null
        return decodeBase64Flexible(payloadPart)?.decodeToString()
    }

    private fun decodeBase64Flexible(input: String): ByteArray? {
        val padded =
            if (input.length % BASE64_BLOCK_SIZE == 0) {
                input
            } else {
                val padAmount = BASE64_BLOCK_SIZE - (input.length % BASE64_BLOCK_SIZE)
                input.padEnd(input.length + padAmount, BASE64_PAD_CHAR)
            }
        return runCatching { Base64.UrlSafe.decode(padded) }
            .recoverCatching { Base64.decode(padded) }
            .getOrNull()
    }
}

data class RootState(
    val initialAnimationComplete: Boolean = false,
    val sessionState: SessionState = SessionState.Loading,
    val error: RootError? = null,
    val navigationTarget: NavigationTarget = NavigationTarget.Splash,
    val isLoginMandatory: Boolean = false,
    val isPendingLogin: UiState<Boolean> = UiState.Initial,
    val accountDialogInfo: AccountDialogInfo? = null,
    val showAccountDialog: Boolean = false,
    val isAccountSwitchInProgress: Boolean = false,
)

sealed interface RootEvent {
    data class ShowDailyStreakCelebration(
        val streakCount: Long,
    ) : RootEvent
}

data class AccountDialogInfo(
    val mainAccount: AccountUi?,
    val botAccounts: List<AccountUi>,
)

data class AccountUi(
    val principal: String,
    val name: String,
    val avatarUrl: String,
    val isBot: Boolean,
    val isActive: Boolean,
)

private fun AccountDirectoryProfile.toAccountUi(activePrincipal: String?): AccountUi =
    AccountUi(
        principal = principal,
        name = username,
        avatarUrl = avatarUrl,
        isBot = isBot,
        isActive = principal == activePrincipal,
    )
