package com.yral.shared.features.account.viewmodel

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.featureflag.WalletFeatureFlags
import com.yral.featureflag.accountFeatureFlags.AccountFeatureFlags
import com.yral.featureflag.accountFeatureFlags.AccountLinksDto
import com.yral.shared.analytics.events.MenuCtaType
import com.yral.shared.core.AppConfigurations.CHAT_BASE_URL
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.utils.getAccountInfo
import com.yral.shared.features.account.analytics.AccountsTelemetry
import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.domain.useCases.DeleteAccountUseCase
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.firebaseStore.getDownloadUrl
import com.yral.shared.http.httpDelete
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import dev.gitlive.firebase.storage.FirebaseStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AccountEnv(
    val isDebug: Boolean,
)

@Suppress("LongParameterList")
class AccountsViewModel internal constructor(
    private val appDispatchers: AppDispatchers,
    private val authClientFactory: AuthClientFactory,
    private val sessionManager: SessionManager,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    val accountsTelemetry: AccountsTelemetry,
    private val flagManager: FeatureFlagManager,
    private val firebaseStorage: FirebaseStorage,
    private val preferences: Preferences,
    private val httpClient: HttpClient,
    private val accountEnv: AccountEnv,
    private val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase,
    private val deregisterNotificationTokenUseCase: DeregisterNotificationTokenUseCase,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.disk)
    private val json = Json { ignoreUnknownKeys = true }

    private val authClient =
        authClientFactory
            .create(coroutineScope) { e ->
                Logger.e("Auth error - $e")
                handleSignupFailed()
            }

    private val _state =
        MutableStateFlow(
            value =
                AccountsState(
                    accountInfo = sessionManager.getAccountInfo(),
                    accountLinks = flagManager.get(AccountFeatureFlags.AccountLinks.Links),
                    isWalletEnabled = flagManager.isEnabled(WalletFeatureFlags.Wallet.Enabled),
                    isSubscriptionEnabled = flagManager.isEnabled(AppFeatureFlags.Common.EnableSubscription),
                    isBotAccount = sessionManager.isBotAccount ?: false,
                ),
        )
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init {
        coroutineScope.launch {
            _state.value.accountLinks.supportIcon?.let { supportIcon ->
                getDownloadUrl(supportIcon, firebaseStorage)
                    .takeIf { url -> url.isNotEmpty() }
                    ?.let { iconUrl -> _state.update { it.copy(supportIcon = iconUrl) } }
            }
        }
        coroutineScope.launch {
            val isEnabled = preferences.getBoolean(PrefKeys.NOTIFICATION_ALERTS_ENABLED.name) ?: false
            _state.update { it.copy(alertsEnabled = isEnabled) }
        }
        coroutineScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    _state.update { it.copy(isLoggedIn = isSocialSignIn) }
                }
        }
        coroutineScope.launch {
            sessionManager
                .observeSessionState(transform = { sessionManager.getAccountInfo() })
                .collect { info: AccountInfo? ->
                    _state.update {
                        it.copy(
                            accountInfo = info,
                            isBotAccount = sessionManager.isBotAccount ?: false,
                        )
                    }
                }
        }
    }

    private fun logout() {
        coroutineScope.launch {
            authClient.logout()
        }
    }

    @Suppress("LongMethod")
    fun deleteAccount(onBotDeleted: () -> Unit = {}) {
        coroutineScope.launch {
            val activePrincipal = sessionManager.userPrincipal
            val isBotAccount = sessionManager.isBotAccount == true
            Logger.d(BOT_DELETE_LOG_TAG) {
                "deleteAccount invoked principal=$activePrincipal isBot=$isBotAccount"
            }
            if (isBotAccount) {
                _state.update { it.copy(isDeletingAccount = true) }
                Logger.d(BOT_DELETE_LOG_TAG) { "bot delete: loader shown" }
                softDeleteInfluencerOnBotServer(activePrincipal)
            }
            deleteAccountUseCase
                .invoke(Unit)
                .onSuccess {
                    Logger.d(BOT_DELETE_LOG_TAG) { "deleteAccount usecase success result=$it" }
                    if (isBotAccount && activePrincipal != null) {
                        Logger.d(BOT_DELETE_LOG_TAG) { "delete_user_info success principalToDelete=$activePrincipal" }
                        removeDeletedBotFromLocalCaches(activePrincipal)
                        Logger.d(BOT_DELETE_LOG_TAG) { "bot delete: local cache cleanup completed" }
                        withContext(appDispatchers.main) {
                            onBotDeleted()
                        }
                        Logger.d(BOT_DELETE_LOG_TAG) { "bot delete: switchToMain callback dispatched" }
                        coroutineScope.launch {
                            authClient.refreshTokensAfterBotDeletion()
                        }
                    } else {
                        Logger.d(BOT_DELETE_LOG_TAG) { "main account delete: triggering logout" }
                        logout()
                    }
                }.onFailure {
                    _state.update { current -> current.copy(isDeletingAccount = false) }
                    Logger.e(BOT_DELETE_LOG_TAG, it) {
                        "deleteAccount failed principal=$activePrincipal isBot=$isBotAccount message=${it.message}"
                    }
                    Logger.e("Failed to delete account: ${it.message}")
                    setBottomSheetType(
                        type = AccountBottomSheet.ErrorMessage(ErrorType.DELETE_ACCOUNT_FAILED),
                    )
                }
        }
    }

    private suspend fun softDeleteInfluencerOnBotServer(principal: String?) {
        if (principal.isNullOrBlank()) {
            Logger.w(BOT_DELETE_LOG_TAG) { "bot-server soft delete skipped: principal missing" }
            return
        }
        val idToken = preferences.getString(PrefKeys.ID_TOKEN.name)
        if (idToken.isNullOrBlank()) {
            Logger.w(BOT_DELETE_LOG_TAG) {
                "bot-server soft delete skipped: id token missing principal=$principal"
            }
            return
        }
        val environmentPrefix = if (accountEnv.isDebug) "staging" else ""
        Logger.d(BOT_DELETE_LOG_TAG) {
            "calling bot-server soft delete influencerId=$principal envPrefix=$environmentPrefix"
        }
        runCatching {
            httpDelete(httpClient) {
                url {
                    host = CHAT_BASE_URL
                    path(environmentPrefix, SOFT_DELETE_INFLUENCER_PATH, principal)
                }
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
        }.onSuccess { response ->
            Logger.d(BOT_DELETE_LOG_TAG) {
                "bot-server soft delete success influencerId=$principal response=$response"
            }
        }.onFailure { error ->
            Logger.w(BOT_DELETE_LOG_TAG, error) {
                "bot-server soft delete failed; continuing with delete_user_info influencerId=$principal " +
                    "message=${error.message}"
            }
        }
    }

    fun onBotSwitchToMainFinished(success: Boolean) {
        if (!success) {
            _state.update { it.copy(isDeletingAccount = false) }
            setBottomSheetType(
                type = AccountBottomSheet.ErrorMessage(ErrorType.DELETE_ACCOUNT_FAILED),
            )
        }
    }

    private fun handleSignupFailed() {
        setBottomSheetType(
            type = AccountBottomSheet.ErrorMessage(ErrorType.SIGNUP_FAILED),
        )
    }

    fun setBottomSheetType(type: AccountBottomSheet) {
        _state.update { it.copy(bottomSheetType = type) }
    }

    fun getHelperLinks(): List<AccountHelpLink> {
        val accountLinks = _state.value.accountLinks
        val links =
            mutableListOf(
                AccountHelpLink(
                    type = AccountHelpLinkType.TALK_TO_TEAM,
                    link = accountLinks.support.trim(),
                    linkText = accountLinks.supportText,
                    linkRemoteIcon = _state.value.supportIcon,
                    openInExternalBrowser = true,
                    menuCtaType = MenuCtaType.TALK_TO_THE_TEAM,
                ),
                AccountHelpLink(
                    type = AccountHelpLinkType.TERMS_OF_SERVICE,
                    link = accountLinks.tnc,
                    openInExternalBrowser = false,
                    menuCtaType = MenuCtaType.TERMS_OF_SERVICE,
                ),
                AccountHelpLink(
                    type = AccountHelpLinkType.PRIVACY_POLICY,
                    link = accountLinks.privacyPolicy,
                    openInExternalBrowser = false,
                    menuCtaType = MenuCtaType.PRIVACY_POLICY,
                ),
            )
        if (_state.value.isLoggedIn) {
            links.add(
                AccountHelpLink(
                    type = AccountHelpLinkType.LOGOUT,
                    link = LOGOUT_URI,
                    openInExternalBrowser = true,
                    menuCtaType = MenuCtaType.LOG_OUT,
                ),
            )
            links.add(
                AccountHelpLink(
                    type = AccountHelpLinkType.DELETE_ACCOUNT,
                    link = DELETE_ACCOUNT_URI,
                    openInExternalBrowser = true,
                    menuCtaType = MenuCtaType.DELETE_ACCOUNT,
                ),
            )
        }
        return links
    }

    private suspend fun removeDeletedBotFromLocalCaches(deletedPrincipal: String) {
        Logger.d(BOT_DELETE_LOG_TAG) { "cache cleanup: start deletedPrincipal=$deletedPrincipal" }
        val updatedBots =
            preferences
                .getString(PrefKeys.BOT_IDENTITIES.name)
                ?.let { stored -> runCatching { json.decodeFromString<List<BotIdentityEntry>>(stored) }.getOrNull() }
                .orEmpty()
                .filterNot { entry -> entry.principal == deletedPrincipal }
        preferences.putString(PrefKeys.BOT_IDENTITIES.name, json.encodeToString(updatedBots))
        sessionManager.updateBotCount(updatedBots.size)
        Logger.d(BOT_DELETE_LOG_TAG) { "cache cleanup: bot identities updated count=${updatedBots.size}" }

        val updatedDirectory =
            sessionManager.accountDirectory?.let { directory ->
                directory.copy(
                    botPrincipals = directory.botPrincipals.filterNot { it == deletedPrincipal },
                    profilesByPrincipal = directory.profilesByPrincipal - deletedPrincipal,
                )
            }
        sessionManager.updateAccountDirectory(updatedDirectory)
        preferences.remove(PrefKeys.ACCOUNT_DIRECTORY_CACHE.name)
        Logger.d(BOT_DELETE_LOG_TAG) { "cache cleanup: account directory updated and cache cleared" }

        val lastActive = preferences.getString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name)
        val mainPrincipal = preferences.getString(PrefKeys.MAIN_PRINCIPAL.name)
        if (lastActive == deletedPrincipal && mainPrincipal != null) {
            preferences.putString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name, mainPrincipal)
            Logger.d(BOT_DELETE_LOG_TAG) {
                "cache cleanup: last active principal moved to mainPrincipal=$mainPrincipal"
            }
        }
        Logger.d(BOT_DELETE_LOG_TAG) { "cache cleanup: complete deletedPrincipal=$deletedPrincipal" }
    }

    fun getSocialLinks(): List<AccountHelpLink> {
        val accountLinks = _state.value.accountLinks
        return listOf(
            AccountHelpLink(
                type = AccountHelpLinkType.TELEGRAM,
                link = accountLinks.telegram,
                openInExternalBrowser = true,
                menuCtaType = MenuCtaType.FOLLOW_ON,
            ),
            AccountHelpLink(
                type = AccountHelpLinkType.DISCORD,
                link = accountLinks.discord,
                openInExternalBrowser = true,
                menuCtaType = MenuCtaType.FOLLOW_ON,
            ),
            AccountHelpLink(
                type = AccountHelpLinkType.TWITTER,
                link = accountLinks.twitter,
                openInExternalBrowser = true,
                menuCtaType = MenuCtaType.FOLLOW_ON,
            ),
        )
    }

    fun handleHelpLink(link: AccountHelpLink) {
        when (link.link) {
            LOGOUT_URI -> logout()
            DELETE_ACCOUNT_URI -> setBottomSheetType(AccountBottomSheet.DeleteAccount)
            else ->
                setBottomSheetType(
                    AccountBottomSheet.ShowWebView(
                        linkToOpen = link,
                    ),
                )
        }
    }

    fun onAlertsToggleChanged(isEnabled: Boolean) {
        coroutineScope.launch {
            preferences.putBoolean(PrefKeys.NOTIFICATION_ALERTS_ENABLED.name, isEnabled)
        }
        _state.update { it.copy(alertsEnabled = isEnabled) }
    }

    suspend fun registerAlerts(): Boolean =
        runCatching {
            registerNotificationTokenUseCase()
        }.onFailure { error ->
            Logger.e("AccountsViewModel") { "Failed to register notifications: ${error.message}" }
        }.isSuccess

    suspend fun deregisterAlerts(): Boolean =
        runCatching {
            deregisterNotificationTokenUseCase()
        }.onFailure { error ->
            Logger.e("AccountsViewModel") { "Failed to deregister notifications: ${error.message}" }
        }.isSuccess

    companion object {
        const val BOT_DELETE_LOG_TAG = "BotDeleteFlow"
        const val LOGOUT_URI = "yral://logout"
        const val DELETE_ACCOUNT_URI = "yral://deleteAccount"
        private const val SOFT_DELETE_INFLUENCER_PATH = "api/v1/influencers"
    }
}

data class AccountHelpLink(
    val type: AccountHelpLinkType,
    val link: String,
    val linkText: String? = null,
    val linkRemoteIcon: String? = null,
    val openInExternalBrowser: Boolean,
    val menuCtaType: MenuCtaType,
)

enum class AccountHelpLinkType {
    TALK_TO_TEAM,
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
    TELEGRAM,
    DISCORD,
    TWITTER,
    LOGOUT,
    DELETE_ACCOUNT,
}

data class AccountsState(
    val accountInfo: AccountInfo?,
    val bottomSheetType: AccountBottomSheet = AccountBottomSheet.None,
    val accountLinks: AccountLinksDto,
    val supportIcon: String? = null,
    val isWalletEnabled: Boolean = false,
    val isSubscriptionEnabled: Boolean = false,
    val isLoggedIn: Boolean = false,
    val alertsEnabled: Boolean = false,
    val isBotAccount: Boolean = false,
    val isDeletingAccount: Boolean = false,
)

sealed interface AccountBottomSheet {
    data object None : AccountBottomSheet
    data class ShowWebView(
        val linkToOpen: AccountHelpLink,
    ) : AccountBottomSheet
    data object DeleteAccount : AccountBottomSheet
    data class ErrorMessage(
        val errorType: ErrorType,
    ) : AccountBottomSheet
}

enum class ErrorType {
    SIGNUP_FAILED,
    DELETE_ACCOUNT_FAILED,
}

@Serializable
private data class BotIdentityEntry(
    val principal: String,
    val identity: String,
    val username: String? = null,
)
